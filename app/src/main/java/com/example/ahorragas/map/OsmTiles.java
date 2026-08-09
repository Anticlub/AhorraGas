package com.example.ahorragas.map;

import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.TileSourcePolicy;
import org.osmdroid.tileprovider.tilesource.XYTileSource;

/**
 * Fuentes de tiles de OpenStreetMap para la app.
 *
 * <p>Se define una fuente propia en lugar de {@code TileSourceFactory.MAPNIK}
 * porque la política de MAPNIK tiene activado {@code FLAG_USER_AGENT_NORMALIZED}:
 * eso hace que osmdroid ignore el User-Agent que fijamos y use uno "normalizado"
 * a partir del nombre del paquete ({@code com.example.ahorragas/1}), que los
 * servidores de OpenStreetMap <b>bloquean</b> (devuelven una imagen de
 * "Access blocked").</p>
 *
 * <p>Esta fuente usa la misma URL de tiles pero con una política SIN
 * {@code FLAG_USER_AGENT_NORMALIZED}. Así osmdroid respeta el User-Agent válido
 * que fija {@code AhorraGasApp} ("AhorraGas/1.0 ..."), que OSM sí acepta.
 * Ver política de uso: https://operations.osmfoundation.org/policies/tiles/</p>
 */
public final class OsmTiles {

    private OsmTiles() {
    }

    /**
     * Tiles estándar de OpenStreetMap (equivalente a MAPNIK) con User-Agent
     * propio respetado. El nombre es distinto de "Mapnik" a propósito, para no
     * reutilizar el caché en disco donde pudieran haber quedado tiles de
     * "Access blocked" descargadas con el User-Agent antiguo.
     */
    public static final ITileSource OPENSTREETMAP = new XYTileSource(
            "OpenStreetMap",
            0, 19, 256, ".png",
            new String[]{"https://tile.openstreetmap.org/"},
            "© OpenStreetMap contributors",
            new TileSourcePolicy(2,
                    TileSourcePolicy.FLAG_NO_BULK
                            | TileSourcePolicy.FLAG_NO_PREVENTIVE
                            | TileSourcePolicy.FLAG_USER_AGENT_MEANINGFUL));
}
