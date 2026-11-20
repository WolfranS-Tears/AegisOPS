# database

Scripts SQL que PostgreSQL ejecuta al crear el contenedor por primera vez (orden alfabético), por ejemplo:

- `01_schema.sql` — tablas, claves foráneas, restricciones e índices.
- `02_seed.sql` — datos iniciales (categorías, usuarios, artículos, incidentes).

Solo se ejecutan con el volumen vacío. Para reaplicarlos: `docker compose down -v` y `docker compose up -d` (borra los datos).
