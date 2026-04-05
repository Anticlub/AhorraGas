package com.example.ahorragas.data;

import android.util.Xml;

import com.example.ahorragas.model.ConnectorType;
import com.example.ahorragas.model.Electrolinera;
import com.example.ahorragas.util.GeoValidation;

import org.xmlpull.v1.XmlPullParser;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class RemoteDgtDataSource implements ElectrolineraDataSource {

    private static final String ENDPOINT =
            "https://infocar.dgt.es/datex2/v3/miterd/" +
                    "EnergyInfrastructureTablePublication/electrolineras.xml";

    @Override
    public List<Electrolinera> loadElectrolineras() throws RepoError {
        HttpURLConnection connection = null;
        InputStream is = null;

        try {
            URL url = new URL(ENDPOINT);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(60000); // XML grande → timeout generoso
            connection.setRequestProperty("User-Agent", "Ahorragas/1.0 (Android)");

            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new RepoError(RepoError.Type.HTTP, code, "HTTP " + code);
            }

            is = connection.getInputStream();
            List<Electrolinera> result = parseXml(is);

            if (result.isEmpty()) {
                throw new RepoError(RepoError.Type.EMPTY_RESPONSE,
                        "El XML de electrolineras no contiene estaciones válidas");
            }

            return result;

        } catch (SocketTimeoutException e) {
            throw new RepoError(RepoError.Type.TIMEOUT,
                    "Timeout descargando electrolineras DGT");
        } catch (RepoError e) {
            throw e;
        } catch (Exception e) {
            throw new RepoError(RepoError.Type.NETWORK,
                    "Fallo de red electrolineras: " + e.getMessage());
        } finally {
            try { if (is != null) is.close(); } catch (Exception ignored) {}
            if (connection != null) connection.disconnect();
        }
    }

    private List<Electrolinera> parseXml(InputStream is) throws RepoError {
        List<Electrolinera> result = new ArrayList<>();
        Electrolinera current = null;
        Electrolinera.Conector connectorEnCurso = null;

        // Flags de contexto
        boolean inName          = false;
        boolean inOperator      = false;
        boolean inCoordinates   = false;
        boolean inConnector     = false;
        boolean inAddressLine   = false;
        boolean nombreSiteAsignado   = false;
        boolean operadorAsignado     = false;
        int addressOrder        = -1;

        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
            parser.setInput(is, "UTF-8");

            int eventType = parser.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                String localName = parser.getName();
                if (localName == null) localName = "";

                if (eventType == XmlPullParser.START_TAG) {
                    switch (localName) {
                        case "energyInfrastructureSite":
                            current = new Electrolinera();
                            current.setId(parser.getAttributeValue(null, "id"));
                            nombreSiteAsignado = false;
                            operadorAsignado   = false;
                            break;
                        case "name":
                            inName = true;
                            break;
                        case "operator":
                            inOperator = true;
                            break;
                        case "label":
                            // horario — se lee en TEXT
                            break;
                        case "coordinatesForDisplay":
                            inCoordinates = true;
                            break;
                        case "addressLine":
                            inAddressLine = true;
                            String order = parser.getAttributeValue(null, "order");
                            addressOrder = order != null ? Integer.parseInt(order) : -1;
                            break;
                        case "connector":
                            inConnector = true;
                            connectorEnCurso = new Electrolinera.Conector(
                                    ConnectorType.UNKNOWN, null, null);
                            break;
                    }

                } else if (eventType == XmlPullParser.END_TAG) {
                    switch (localName) {
                        case "energyInfrastructureSite":
                            if (current != null &&
                                    GeoValidation.isValidLatLon(current.getLat(), current.getLon())) {
                                result.add(current);
                            }
                            current = null;
                            break;
                        case "name":
                            inName = false;
                            break;
                        case "operator":
                            inOperator = false;
                            break;
                        case "addressLine":
                            inAddressLine = false;
                            addressOrder  = -1;
                            break;
                        case "coordinatesForDisplay":
                            inCoordinates = false;
                            break;
                        case "connector":
                            if (current != null && connectorEnCurso != null) {
                                current.addConector(connectorEnCurso);
                            }
                            connectorEnCurso = null;
                            inConnector      = false;
                            break;
                    }

                } else if (eventType == XmlPullParser.TEXT && current != null) {
                    String text = parser.getText();
                    if (text == null) {
                        eventType = parser.next();
                        continue;
                    }
                    text = text.trim();
                    if (text.isEmpty()) {
                        eventType = parser.next();
                        continue;
                    }

                    // Nombre del site — primer <value> dentro de <name> fuera de operator
                    if ("value".equals(localName) && inName && !inOperator && !nombreSiteAsignado) {
                        current.setNombre(text);
                        nombreSiteAsignado = true;
                    }

                    // Operador — primer <value> dentro de <operator>
                    if ("value".equals(localName) && inOperator && !operadorAsignado) {
                        current.setOperador(text);
                        operadorAsignado = true;
                    }

                    // Horario
                    if ("label".equals(localName)) {
                        current.setHorario(text.trim());
                    }

                    // Coordenadas
                    if (inCoordinates) {
                        if ("latitude".equals(localName)) {
                            try { current.setLat(Double.parseDouble(text)); }
                            catch (NumberFormatException ignored) {}
                        } else if ("longitude".equals(localName)) {
                            try { current.setLon(Double.parseDouble(text)); }
                            catch (NumberFormatException ignored) {}
                        }
                    }

                    // Dirección — las addressLine vienen como "Dirección: Camí dels Reis 166"
                    if ("value".equals(localName) && inAddressLine) {
                        switch (addressOrder) {
                            case 1: current.setDireccion(stripPrefix(text, "Dirección:")); break;
                            case 2: current.setMunicipio(stripPrefix(text, "Municipio:")); break;
                            case 3: current.setProvincia(stripPrefix(text, "Provincia:")); break;
                        }
                    }

                    // Conector
                    if (inConnector && connectorEnCurso != null) {
                        switch (localName) {
                            case "connectorType":
                                connectorEnCurso = new Electrolinera.Conector(
                                        ConnectorType.fromXmlValue(text),
                                        connectorEnCurso.getModoRecarga(),
                                        connectorEnCurso.getPotenciaW());
                                break;
                            case "chargingMode":
                                connectorEnCurso = new Electrolinera.Conector(
                                        connectorEnCurso.getTipo(),
                                        text,
                                        connectorEnCurso.getPotenciaW());
                                break;
                            case "maxPowerAtSocket":
                                try {
                                    connectorEnCurso = new Electrolinera.Conector(
                                            connectorEnCurso.getTipo(),
                                            connectorEnCurso.getModoRecarga(),
                                            Double.parseDouble(text));
                                } catch (NumberFormatException ignored) {}
                                break;
                        }
                    }
                }

                eventType = parser.next();
            }

        } catch (Exception e) {
            throw new RepoError(RepoError.Type.PARSE,
                    "Error parseando XML de electrolineras: " + e.getMessage());
        }

        return result;
    }

    /**
     * Elimina el prefijo de las líneas de dirección del XML.
     * Ejemplo: "Dirección: Camí dels Reis 166" → "Camí dels Reis 166"
     *
     * @param text   texto completo de la línea
     * @param prefix prefijo a eliminar (ej: "Dirección:")
     * @return texto sin el prefijo
     */
    private String stripPrefix(String text, String prefix) {
        if (text == null) return "";
        int idx = text.indexOf(prefix);
        if (idx >= 0) return text.substring(idx + prefix.length()).trim();
        return text.trim();
    }
}