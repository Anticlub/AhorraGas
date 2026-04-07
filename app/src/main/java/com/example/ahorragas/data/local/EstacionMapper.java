package com.example.ahorragas.data.local;

import com.example.ahorragas.model.ConnectorType;
import com.example.ahorragas.model.Electrolinera;
import com.example.ahorragas.model.FuelType;
import com.example.ahorragas.model.Gasolinera;

import java.util.ArrayList;
import java.util.List;

/**
 * Convierte entre modelos de dominio ({@link Gasolinera}, {@link Electrolinera})
 * y entidades Room ({@link EstacionEntity}, {@link ConectorEntity}).
 */
public final class EstacionMapper {

    private EstacionMapper() {}

    // ─── Gasolinera → Entity ──────────────────────────────────────────────────

    /**
     * Convierte una {@link Gasolinera} a su {@link EstacionEntity} para Room.
     *
     * @param g          gasolinera de dominio
     * @param updatedAt  timestamp Unix de la descarga
     * @return entidad lista para insertar en Room
     */
    public static EstacionEntity fromGasolinera(Gasolinera g, long updatedAt) {
        EstacionEntity e = new EstacionEntity();
        e.stationId  = String.valueOf(g.getId());
        e.marca      = g.getMarca();
        e.municipio  = g.getMunicipio();
        e.direccion  = g.getDireccion();
        e.horario    = g.getHorario();
        e.lat        = g.getLat();
        e.lon        = g.getLon();
        e.esElectrica = false;
        e.operador   = null;
        e.updatedAt  = updatedAt;

        e.precioGasolina95        = g.getPrecio(FuelType.GASOLINA_95_E5);
        e.precioGasolina95Premium = g.getPrecio(FuelType.GASOLINA_95_E5_PREMIUM);
        e.precioGasolina98        = g.getPrecio(FuelType.GASOLINA_98_E5);
        e.precioGasoleoA          = g.getPrecio(FuelType.GASOLEO_A);
        e.precioGasoleoPremium    = g.getPrecio(FuelType.GASOLEO_PREMIUM);
        e.precioGasoleoB          = g.getPrecio(FuelType.GASOLEO_B);
        e.precioGasoleoC          = g.getPrecio(FuelType.GASOLEO_C);
        e.precioBiodiesel         = g.getPrecio(FuelType.BIODIESEL);
        e.precioBioetanol         = g.getPrecio(FuelType.BIOETANOL);
        e.precioGlp               = g.getPrecio(FuelType.GLP);
        e.precioGnc               = g.getPrecio(FuelType.GNC);

        return e;
    }

    /**
     * Convierte una {@link Electrolinera} a su {@link EstacionEntity} para Room.
     *
     * @param el         electrolinera de dominio
     * @param updatedAt  timestamp Unix de la descarga
     * @return entidad lista para insertar en Room
     */
    public static EstacionEntity fromElectrolinera(Electrolinera el, long updatedAt) {
        EstacionEntity e = new EstacionEntity();
        e.stationId   = el.getId();
        e.marca       = el.getNombre();
        e.municipio   = el.getMunicipio();
        e.direccion   = el.getDireccion();
        e.horario     = el.getHorario();
        e.lat         = el.getLat();
        e.lon         = el.getLon();
        e.esElectrica = true;
        e.operador    = el.getOperador();
        e.updatedAt   = updatedAt;
        return e;
    }

    /**
     * Convierte los conectores de una {@link Electrolinera} a lista de {@link ConectorEntity}.
     *
     * @param el electrolinera de dominio
     * @return lista de entidades de conector listas para insertar en Room
     */
    public static List<ConectorEntity> conectoresFromElectrolinera(Electrolinera el) {
        List<ConectorEntity> result = new ArrayList<>();
        if (el.getConectores() == null) return result;
        for (Electrolinera.Conector c : el.getConectores()) {
            ConectorEntity ce = new ConectorEntity();
            ce.estacionId  = el.getId();
            ce.tipo        = c.getTipo() != null ? c.getTipo().name() : ConnectorType.UNKNOWN.name();
            ce.modoRecarga = c.getModoRecarga();
            ce.potenciaW   = c.getPotenciaW();
            result.add(ce);
        }
        return result;
    }

    // ─── Entity → Gasolinera ──────────────────────────────────────────────────

    /**
     * Convierte una {@link EstacionEntity} de gasolinera a modelo de dominio {@link Gasolinera}.
     *
     * @param e entidad de Room
     * @return gasolinera de dominio
     */
    public static Gasolinera toGasolinera(EstacionEntity e) {
        Gasolinera g = new Gasolinera();
        g.setMarca(e.marca);
        g.setMunicipio(e.municipio);
        g.setDireccion(e.direccion);
        g.setHorario(e.horario);
        g.setLat(e.lat);
        g.setLon(e.lon);

        g.setPrecio(FuelType.GASOLINA_95_E5,         e.precioGasolina95);
        g.setPrecio(FuelType.GASOLINA_95_E5_PREMIUM, e.precioGasolina95Premium);
        g.setPrecio(FuelType.GASOLINA_98_E5,         e.precioGasolina98);
        g.setPrecio(FuelType.GASOLEO_A,              e.precioGasoleoA);
        g.setPrecio(FuelType.GASOLEO_PREMIUM,        e.precioGasoleoPremium);
        g.setPrecio(FuelType.GASOLEO_B,              e.precioGasoleoB);
        g.setPrecio(FuelType.GASOLEO_C,              e.precioGasoleoC);
        g.setPrecio(FuelType.BIODIESEL,              e.precioBiodiesel);
        g.setPrecio(FuelType.BIOETANOL,              e.precioBioetanol);
        g.setPrecio(FuelType.GLP,                    e.precioGlp);
        g.setPrecio(FuelType.GNC,                    e.precioGnc);

        return g;
    }

    /**
     * Convierte una {@link EstacionEntity} de electrolinera + sus {@link ConectorEntity}
     * a modelo de dominio {@link Gasolinera} con isElectric=true.
     *
     * @param e         entidad de Room
     * @param conectores lista de conectores asociados
     * @return gasolinera eléctrica de dominio
     */
    public static Gasolinera toGasolineraElectrica(EstacionEntity e,
                                                   List<ConectorEntity> conectores) {
        Gasolinera g = new Gasolinera();
        g.setMarca(e.marca);
        g.setOperador(e.operador);
        g.setMunicipio(e.municipio);
        g.setDireccion(e.direccion);
        g.setHorario(e.horario);
        g.setLat(e.lat);
        g.setLon(e.lon);
        g.setElectric(true);
        g.setPrecio(FuelType.ELECTRICO, 0.0);

        if (conectores != null) {
            List<Electrolinera.Conector> lista = new ArrayList<>();
            for (ConectorEntity ce : conectores) {
                ConnectorType tipo;
                try { tipo = ConnectorType.valueOf(ce.tipo); }
                catch (Exception ex) { tipo = ConnectorType.UNKNOWN; }
                lista.add(new Electrolinera.Conector(tipo, ce.modoRecarga, ce.potenciaW));
            }
            g.setConectores(lista);
        }

        return g;
    }
}