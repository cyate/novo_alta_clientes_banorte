# Dominio de Negocio — Proceso Batch de Alta de Clientes Banorte

Este documento describe el dominio de negocio del proceso batch de altas (y operaciones asociadas) para empresas, usuarios, cuentas y lotes de tarjetas. Se centra en reglas de negocio, flujos, puntos de control y criterios de aceptación. Complementa al README general del proyecto y sirve de guía para migraciones de plataforma (por ejemplo, a Go) sin perder semántica de negocio.

## 1. Objetivo del proceso
- Recibir un archivo de entrada diario con solicitudes de alta/subsecuente/reactivación.
- Validar consistencia e integridad de la información.
- Ejecutar las operaciones en BD (Oracle) de acuerdo con reglas de negocio y modalidad de solicitud.
- Generar los archivos SGC y el archivo de respuesta para consumo externo.
- Notificar resultados y alertas por correo.

## 2. Entidades principales (glosario)
- Empresa: cliente corporativo. Identificada por `NVP-CTA-ADMIN` (RIF/CIRIF) y `NVP-NUM-EMPRESA`.
- Usuario: usuario BEM/BEP asociado a la empresa (`NVP-USUARIO-BEM-BEP`, `NVP-CURP`).
- Cuenta Maestra: cuenta de depósito de la empresa (`NVP-CUENTA`) y saldos asociados.
- Lote de tarjetas: emisión de tarjetas (nominadas/innominadas) por lote.
- Tarjeta: registro individual en el detalle del archivo (campos como `NVP-NUM-TAR`, `NVP-IND-EMB`, `NVP-FEC-EXPIRACION`, etc.).
- IdServicioG (GENERAL) e IdServicioD (DETAIL): identificadores lógicos que combinan cuenta/empresa/tipo y stock.
- Stock: secuencia del lote dentro del IdServicio (p.ej. `0000`, `0001`, ...).
- Motivo: tipo de operación de negocio
  - `A` = Alta (primera vez)
  - `M` = Subsecuente (altas posteriores asociadas a la misma empresa/cuenta)
- Indicador de servicio (`NVP-INDSRVNA`):
  - `N` = Modelo nuevo (la cuenta relevante es `NVP-CTA-ADMIN`)
  - `A` = Modelo actual/anterior (la cuenta relevante es `NVP-CUENTA`)
- Reactivación: operación que cambia estatus de inactivo a activo para empresa/usuario existentes.
- Solo lote: caso especial en Alta `A` cuando la empresa ya existe activa y el stock es el primero (`0001`).

## 3. Entradas y salidas
Entradas
- Archivo de entrada (arcentrada): nombre parametrizado por `FILE_NAME` + `%%ODATE` + `FILE_EXT` en `config/constant_config.properties`.
- Estructura: registros `GENERAL`, `DETAIL` y `CONTROL` con separador `FIELD_SEPARATOR`.

Salidas
- Archivos SGC: lista definida por `LIST_FILES` en `constant_config.properties`, ubicados en `FILE_PATH_SGC` con extensión `FILE_EXT`.
- Archivo de respuesta: `FILE_PATH_RESPUESTA` + `FILE_NAME_OUTPUT` (con `%%ODATE`) + `FILE_EXT`.
- Notificaciones por correo (éxito/resumen o alertas).

## 4. Flujo de negocio (alto nivel)
1) Validaciones de formato y contenido del archivo de entrada (estructura y conteos).
2) Construcción de listas en memoria: Empresas y Tarjetas.
3) Validaciones contra BD: existencia/estatus de empresa/usuario/cuenta, duplicidades, consistencia IdServicio, reglas de reactivación y solo lote.
4) Orquestación transaccional:
   - Procesar Empresa → Cuenta Maestra → Usuario → Lotes/Tarjetas.
   - Generar archivos SGC y validar su existencia/tamaño.
   - Generar archivo de respuesta y validarlo.
   - En éxito: COMMIT; en fallo: ROLLBACK + alerta.
5) Notificaciones externas (a usuarios) e internas (resumen/alertas).

## 5. Reglas de negocio clave
Validaciones previas
- Conteos: `NVP-NUM-REG` (cantidad de tarjetas) debe ser consistente con `NVP-NUM-STOCK-G` y detalle.
- Composición de `IdServicioG`: debe corresponder a cuenta + empresa + tipo de empresa:
  - Si `INDSRVNA = N`: prefijo de `IdServicioG` = `NVP-CTA-ADMIN`.
  - Si `INDSRVNA = A`: prefijo de `IdServicioG` = `NVP-CUENTA`.
  - Segmentos intermedios coinciden con `NVP-NUM-EMPRESA` y el dígito de tipo de empresa.
- Campos de cuenta sin espacios: `NVP-CTA-ADMIN` (N) o `NVP-CUENTA` (A).
- El indicador de servicio solo admite `A` o `N` y no puede ser vacío.

Decisiones por modalidad
- Alta (motivo = `A`):
  - Si la empresa no existe activa → insertar maestros (empresa, dirección, sucursal, asignaciones de producto/comisiones/impuestos/parametría) y crear cuenta maestra, usuario y lote.
  - Si la empresa existe activa y es el primer stock (`0001`) → “solo lote” (no se reinsertan empresa/cuenta/usuario).
  - Si existe activa pero no es `0001` → rechazo (inconsistencia de alta).
  - Si existe inactiva → reactivación (ver abajo).
- Subsecuente (motivo = `M`):
  - Requiere que la empresa/cuenta exista y esté activa; de lo contrario, rechazo.
- Reactivación:
  - Empresa inactiva: actualizar estatus a activo y normalizar datos; opcionalmente registrar solicitud de emisión si stock `0000`.
  - Usuario inactivo: actualizar estatus a activo y re-asociar a empresa/producto.

Reglas de lotes/tarjetas
- Lote creado con `TIPO_LOTE`, `STATUS_LOTE`, `PRODUCT` de configuración.
- Nominadas vs innominadas: determinan consultas de lote para subsecuentes.
- Para cada tarjeta válida se verifican duplicidades (número de cuenta) antes de crear maestros.

Transaccionalidad y recuperación
- Toda la etapa de BD y generación/validación de archivos se ejecuta dentro de una transacción global.
- Si cualquier validación posterior a la escritura (SGC/Respuesta) falla → ROLLBACK y envío de alerta.

## 6. Mapeo funcional a operaciones SQL (por fase)
Empresa (Alta)
- Insertar en: `MAESTRO_CLIENTES_TEBCA`, `TEB_DIRECCIONES`, `TEB_SUC_EMPRESA`, `TEB_EMPRES_PRODUC`, `TEB_COMISION`, opcional `TEB_EXONERA_COMISION`.
- Impuestos por `LISTA_TIPO_ASIG_LOTE` en `IMPUESTO_EMPRESA`.
- Si `NVP-NUM-STOCK-G = '0000'`: `SOLICITUD_EMISION_BNT` (SEC-STOCK=0).
- Parámetros de recarga: `TEB_PARAMETERS_EMPRESAS` (por `LISTA_ID_PARAMETRO`).

Empresa (Reactivación)
- `UPDATE MAESTRO_CLIENTES_TEBCA` a activo y metadatos de contacto.
- Si `NVP-NUM-STOCK-G = '0000'`: registrar solicitud en `SOLICITUD_EMISION_BNT`.

Cuenta Maestra
- Alta: `INSERT MAESTRO_DEPOSITO`, `INSERT DETALLE_DEPOSITO` y `DETALLE_DEPOSITO_EN`, `INSERT NOVO_CMAESTRO_PARAMETROS` por cada tipo de transacción, `INSERT EMPRESA_GRUPO`.
- Reactivación: `UPDATE MAESTRO_DEPOSITO` y nuevo registro de detalle de depósito.

Usuario
- Alta: si no existe, `INSERT USUARIOS` + permisos (`NOVO_EOL_UP`) y asociaciones `EMPRESA_GR_USUARIO` y `USUARIO_PRODUCTO`.
- Reactivación: `UPDATE USUARIOS` a activo + asociaciones (empresa/producto).

Lotes y tarjetas
- Crear `TEB_LOTE` (si motivo = `A`) y vincular orden de servicio (`TEB_PROFORMA` + `TEB_OS_LOTES`).
- Para primer stock (`0001`) en Alta: `SOLICITUD_EMISION_BNT` con ID de lote.
- Emisión detalle (`TEB_LOTE_EMISION_TI`) y maestros por tarjeta (nominado/innominado) con validación de duplicidad.
- Subsecuentes (`M`): localizar lote anterior (innominadas: `TEB_LOTE_EMISION_TI` ∩ `TEB_LOTE` con `CESTATUS='6'`; nominadas: `NOVO_LOTE_EMI` ∩ `TEB_LOTE`) y actualizar estados.

## 7. Puntos de control (checklist)
Previos a BD
- Archivo encontrado y con estructura válida (GENERAL/DETAIL/CONTROL).
- Conteos de tarjetas vs. stock coherentes.
- Composición de `IdServicioG` consistente con cuenta/empresa/tipo.
- Existencia/estatus de empresa/cuenta/usuario según modalidad (Alta/Subsecuente/React).
- Reglas de “solo lote” aplicadas cuando corresponde.

Durante BD (dentro de transacción)
- Confirmar no duplicidad de cuentas/tarjetas antes de insertar.
- Aplicar normalización de `INDSRVNA` a valores de marca (BRAND-2023/2020) según regla vigente.
- Formateo de saldos con punto decimal y limpieza de ceros a la izquierda donde aplique.

Posteriores a BD (antes del commit)
- Archivos SGC existentes y con tamaño > 0 para todos los nombres en `LIST_FILES`.
- Archivo de respuesta generado, existente y con tamaño > 0.
- En fallas: ROLLBACK + alerta por correo.

## 8. Criterios de aceptación (negocio)
- Para cada solicitud con `Respuesta = 'A'` y modalidad válida:
  - Los maestros (empresa/cuenta/usuario) deben existir/actualizarse conforme a la modalidad.
  - Los lotes y detalles deben reflejar el número de tarjetas y metadatos del archivo.
  - Deben existir archivos SGC y de respuesta no vacíos.
  - Deben enviarse notificaciones internas; externas solo para altas aprobadas sin reactivación y con usuario nuevo.

## 9. Notificaciones y alertas
- Externas (usuarios): cuando Alta (`A`) aprobada y el usuario no existía (no reactivación); plantilla HTML con variables.
- Internas (equipo): resumen del proceso (n° empresas/usuarios/tarjetas). Si hubo errores, se usa el canal de alerta con tabla de observaciones.

## 10. Lineamientos para migración de plataforma (ej. a Go) — foco negocio
Principios
- Preservar reglas y condicionales de negocio sin cambios semánticos.
- Mantener una única transacción que abarque operaciones de BD y verificación de artefactos de salida.
- Evitar duplicidades: validar existencia antes de insertar (idempotencia).

Pasos sugeridos (vista negocio)
1) Reproducir validaciones del archivo (estructura, conteos, composición de IdServicio).
2) Replicar validaciones contra BD y estados (existente/activo/inactivo) para empresa, cuenta y usuario.
3) Implementar orquestación por modalidad:
   - Alta: insertar maestros completos y lote (o solo lote si aplica “solo lote”).
   - Subsecuente: validar existencia y procesar lote.
   - Reactivación: actualizar estatus y re-asignar.
4) Generar y validar archivos SGC + respuesta antes del commit.
5) Enviar notificaciones con la misma lógica condicional.

Controles de calidad
- Pruebas unitarias para reglas de negocio (conteos, IdServicio, “solo lote”, reactivaciones).
- Pruebas de integración para repositorios y transacciones.
- Pruebas end-to-end en entorno controlado con datos de ejemplo.

## 11. Pseudocódigo (agnóstico, estilo Go) — orquestación
```
func RunProcesoAlta(cfg Config, db *sql.DB, hoy time.Time) error {
    // 1) Validaciones de entrada
    archivo := buildNombreArchivo(cfg.InputPattern, hoy)
    general, details, control, err := parsearArchivo(archivo, cfg.Separador)
    if err != nil { return err }
    if err := validarEstructura(general, details, control, cfg.Reglas); err != nil { return err }

    // 2) Construcción de listas
    empresas := construirEmpresas(general, details)
    tarjetas := construirTarjetas(details)

    // 3) Validaciones BD (existencia/estatus, solo lote, reactivaciones)
    if err := validarContraBD(db, empresas, tarjetas); err != nil { return err }

    // 4) Transacción
    tx, _ := db.Begin()
    if err := procesarEmpresas(tx, empresas); err != nil { tx.Rollback(); return err }
    if err := procesarCuentas(tx, empresas); err != nil { tx.Rollback(); return err }
    if err := procesarUsuarios(tx, empresas); err != nil { tx.Rollback(); return err }
    if err := procesarLotes(tx, empresas, tarjetas); err != nil { tx.Rollback(); return err }

    // 5) Archivos SGC/Respuesta y validaciones
    if err := generarSGC(cfg, empresas, tarjetas); err != nil { tx.Rollback(); return err }
    if err := validarSGC(cfg); err != nil { tx.Rollback(); return err }
    if err := generarRespuesta(cfg, empresas); err != nil { tx.Rollback(); return err }
    if err := validarRespuesta(cfg); err != nil { tx.Rollback(); return err }

    if err := tx.Commit(); err != nil { return err }
    enviarNotificaciones(cfg, empresas)
    return nil
}
```

## 12. Operación diaria (runbook)
- Verificar parámetros de configuración (rutas, nombres, listas SGC, SMTP, Oracle).
- Colocar el archivo de entrada en `FILE_PATH_ENTRADA` con fecha (`%%ODATE`).
- Ejecutar el proceso y revisar el log.
- Confirmar archivos SGC y respuesta en sus carpetas y el envío de correos.
- En fallas: revisar observaciones en correo de alerta y el log para causas (estructura, conteos, duplicidades, inexistencias, archivos vacíos).

## 13. Consideraciones y riesgos
- Inconsistencias entre conteos y stock → rechazo de la solicitud.
- Duplicidades de cuentas/tarjetas → no insertar; mantener idempotencia.
- Dependencia de archivos SGC/Respuesta: su inexistencia o tamaño 0 implica ROLLBACK.
- Configuración sensible (listas, rutas, prefijos de producto) debe versionarse y revisarse por ambiente.

---
Este documento es referencia de negocio. Para detalles técnicos (claves de queries, clases y propiedades) ver `README.md` y `config/constant_*.properties`.
