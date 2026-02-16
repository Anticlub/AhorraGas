Obtener tipos de combustible (GET): https://sedeaplicaciones.minetur.gob.es/ServiciosRESTCarburantes/PreciosCarburantes/Listados/ProductosPetroliferos/

Obtener Comunidades Autónomas (GET):

https://sedeaplicaciones.minetur.gob.es/ServiciosRESTCarburantes/PreciosCarburantes/Listados/ComunidadesAutonomas/
Obtener Provincias(GET):

https://sedeaplicaciones.minetur.gob.es/ServiciosRESTCarburantes/PreciosCarburantes/Listados/Provincias/
Obtener Municipios de una provincia(GET):

Ejemplo: Madrid (IDProvincia = 28):

https://sedeaplicaciones.minetur.gob.es/ServiciosRESTCarburantes/PreciosCarburantes/Listados/MunicipiosPorProvincia/28
 

Obtener estaciones filtradas por municipio + combustible (GET):

https://sedeaplicaciones.minetur.gob.es/ServiciosRESTCarburantes/PreciosCarburantes/EstacionesTerrestres/FiltroMunicipioProducto/4280/4
 

Campo JSON

Uso en la app

Motivo

Rótulo

Marca de la estación

Mostrar nombre comercial (ej. REPSOL, BP)

Dirección

Dirección textual

Mostrar en lista y detalle

Latitud

Posición en mapa

Necesario para pintar marcador

Longitud (WGS84)

Posición en mapa

Necesario para pintar marcador

PrecioProducto

Precio del combustible seleccionado

Mostrar precio en lista y marker

Horario

Información de apertura

Mostrar en detalle o bottom sheet

 

 

Observaciones técnicas importantes:
Latitud, Longitud y PrecioProducto vienen con coma decimal.

PrecioProducto depende del IDProducto usado en el endpoint.
Es decir, el JSON no indica qué combustible es; eso lo sabes por la URL.

Longitud (WGS84) contiene espacio y paréntesis → debe accederse con notación de string en el DTO.
