package com.example.ahorragas.data.remote;

import android.util.Xml;

import com.example.ahorragas.data.ElectrolineraDataSource;
import com.example.ahorragas.data.RepoError;
import com.example.ahorragas.model.ConnectorType;
import com.example.ahorragas.model.Electrolinera;
import com.example.ahorragas.util.GeoValidation;

import org.xmlpull.v1.XmlPullParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * Fuente de datos remota que descarga electrolineras
 * de la DGT usando Retrofit.
 */
public class RemoteDgtDataSource implements ElectrolineraDataSource {

    private static final String BASE_URL = "https://infocar.dgt.es/";

    private final ElectrolineraApiService apiService;

    public RemoteDgtDataSource() {
        apiService = ApiClient.getInstance().createService(BASE_URL, ElectrolineraApiService.class);
    }

    /**
     * Descarga y parsea la lista de electrolineras desde la DGT.
     *
     * @return lista de electrolineras válidas, nunca null
     * @throws RepoError si hay fallo de red, parseo o respuesta vacía
     */
    @Override
    public List<Electrolinera> loadElectrolineras() throws RepoError {
        try {
            Response<ResponseBody> response = apiService.getElectrolineras().execute();

            if (!response.isSuccessful()) {
                throw new RepoError(RepoError.Type.HTTP, response.code(),
                        "HTTP " + response.code());
            }

            ResponseBody body = response.body();
            if (body == null) {
                throw new RepoError(RepoError.Type.EMPTY_RESPONSE,
                        "Respuesta vacía de electrolineras DGT");
            }

            try (InputStream is = body.byteStream()) {
                List<Electrolinera> result = parseXmlPublic(is);

                if (result.isEmpty()) {
                    throw new RepoError(RepoError.Type.EMPTY_RESPONSE,
                            "El XML de electrolineras no contiene estaciones válidas");
                }

                return result;
            }

        } catch (RepoError e) {
            throw e;
        } catch (java.net.SocketTimeoutException e) {
            throw new RepoError(RepoError.Type.TIMEOUT,
                    "Timeout descargando electrolineras DGT");
        } catch (IOException e) {
            throw new RepoError(RepoError.Type.NETWORK,
                    "Fallo de red electrolineras: " + e.getMessage());
        }
    }

    /**
     * Parsea el XML de electrolineras usando XmlPullParser.
     *
     * @param is InputStream del XML
     * @return lista de electrolineras con coordenadas válidas
     * @throws RepoError si hay error de parseo
     */
    List<Electrolinera> parseXmlPublic(InputStream is) throws RepoError {
        List<Electrolinera> result = new ArrayList<>();
        Electrolinera current = null;
        Electrolinera.Conector connectorEnCurso = null;

        boolean inName        = false;
        boolean inOperator    = false;
        boolean inCoordinates = false;
        boolean inConnector   = false;
        boolean inAddressLine = false;
        boolean inText        = false;
        boolean nombreSiteAsignado = false;
        boolean operadorAsignado   = false;
        int addressOrder      = -1;
        String lastOpenTag    = "";

        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
            parser.setInput(is, "UTF-8");

            int eventType = parser.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                String localName = parser.getName();
                if (localName == null) localName = "";

                if (eventType == XmlPullParser.START_TAG) {
                    lastOpenTag = localName;

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
                        case "coordinatesForDisplay":
                            inCoordinates = true;
                            break;
                        case "addressLine":
                            inAddressLine = true;
                            String order = parser.getAttributeValue(null, "order");
                            addressOrder = order != null ? Integer.parseInt(order) : -1;
                            break;
                        case "text":
                            if (inAddressLine) inText = true;
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
                        case "name":        inName = false; break;
                        case "operator":    inOperator = false; break;
                        case "coordinatesForDisplay": inCoordinates = false; break;
                        case "addressLine":
                            inAddressLine = false;
                            inText        = false;
                            addressOrder  = -1;
                            break;
                        case "text":        inText = false; break;
                        case "connector":
                            if (current != null && connectorEnCurso != null) {
                                current.addConector(connectorEnCurso);
                            }
                            connectorEnCurso = null;
                            inConnector      = false;
                            break;
                    }
                    lastOpenTag = "";

                } else if (eventType == XmlPullParser.TEXT && current != null) {
                    String text = parser.getText();
                    if (text == null) { eventType = parser.next(); continue; }
                    text = text.trim();
                    if (text.isEmpty()) { eventType = parser.next(); continue; }

                    switch (lastOpenTag) {
                        case "value":
                            if (inName && !inOperator && !inAddressLine && !inText
                                    && !nombreSiteAsignado && current != null) {
                                current.setNombre(text);
                                nombreSiteAsignado = true;
                            } else if (inOperator && !operadorAsignado && current != null) {
                                current.setOperador(text);
                                operadorAsignado = true;
                            } else if (inText && current != null) {
                                switch (addressOrder) {
                                    case 1: current.setDireccion(stripPrefix(text, "Dirección:")); break;
                                    case 2: current.setMunicipio(stripPrefix(text, "Municipio:")); break;
                                    case 3: current.setProvincia(stripPrefix(text, "Provincia:")); break;
                                }
                            }
                            break;

                        case "label":
                            if (current != null) current.setHorario(text);
                            break;

                        case "latitude":
                            if (inCoordinates && current != null) {
                                try { current.setLat(Double.parseDouble(text)); }
                                catch (NumberFormatException ignored) {}
                            }
                            break;

                        case "longitude":
                            if (inCoordinates && current != null) {
                                try { current.setLon(Double.parseDouble(text)); }
                                catch (NumberFormatException ignored) {}
                            }
                            break;

                        case "connectorType":
                            if (inConnector && connectorEnCurso != null) {
                                connectorEnCurso = new Electrolinera.Conector(
                                        ConnectorType.fromXmlValue(text),
                                        connectorEnCurso.getModoRecarga(),
                                        connectorEnCurso.getPotenciaW());
                            }
                            break;

                        case "chargingMode":
                            if (inConnector && connectorEnCurso != null) {
                                connectorEnCurso = new Electrolinera.Conector(
                                        connectorEnCurso.getTipo(),
                                        text,
                                        connectorEnCurso.getPotenciaW());
                            }
                            break;

                        case "maxPowerAtSocket":
                            if (inConnector && connectorEnCurso != null) {
                                try {
                                    connectorEnCurso = new Electrolinera.Conector(
                                            connectorEnCurso.getTipo(),
                                            connectorEnCurso.getModoRecarga(),
                                            Double.parseDouble(text));
                                } catch (NumberFormatException ignored) {}
                            }
                            break;
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