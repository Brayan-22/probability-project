# Guía de Dockerización del Frontend

Esta guía explica cómo construir y ejecutar el frontend usando Docker.

## Archivos Creados

- `Dockerfile`: Configuración multi-stage para build y runtime
- `docker-compose.yml`: Orquestación del contenedor frontend
- `nginx.conf`: Configuración de Nginx optimizada para SPAs
- `.dockerignore`: Archivos excluidos del contexto de build
- `.env.production`: Ejemplo de variables de entorno para producción

## Construcción de la Imagen

### Opción 1: Build básico (desarrollo)

```bash
docker build -t frontend-probabilidad .
```

### Opción 2: Build con variables de entorno

```bash
docker build -t frontend-probabilidad \
  --build-arg VITE_KEYCLOAK_URL=http://localhost:8080 \
  --build-arg VITE_KEYCLOAK_REALM=Probabilidad \
  --build-arg VITE_KEYCLOAK_CLIENT_ID=frontend-spa \
  --build-arg VITE_API_BASE=http://localhost:8082/api \
  .
```

### Opción 3: Build para producción

```bash
docker build -t frontend-probabilidad:prod \
  --build-arg VITE_KEYCLOAK_URL=https://auth.tu-dominio.com \
  --build-arg VITE_KEYCLOAK_REALM=Probabilidad \
  --build-arg VITE_KEYCLOAK_CLIENT_ID=frontend-spa \
  --build-arg VITE_API_BASE=https://api.tu-dominio.com/api \
  .
```

## Ejecución del Contenedor

### Usando Docker Run

```bash
docker run -d \
  --name frontend-probabilidad \
  -p 3000:80 \
  frontend-probabilidad
```

La aplicación estará disponible en: http://localhost:3000

### Usando Docker Compose

```bash
# Con variables por defecto
docker-compose up -d

# Con archivo .env personalizado
docker-compose --env-file .env.production up -d
```

## Integración con el Stack Completo

Para integrar el frontend con el backend y Keycloak existentes en `../infra/docker-compose.yml`, agrega este servicio:

```yaml
  frontend:
    build:
      context: ../frontend
      dockerfile: Dockerfile
      args:
        VITE_KEYCLOAK_URL: http://localhost:8080
        VITE_KEYCLOAK_REALM: Probabilidad
        VITE_KEYCLOAK_CLIENT_ID: frontend-spa
        VITE_API_BASE: http://localhost:8082/api
    container_name: frontend-probabilidad
    ports:
      - "3000:80"
    restart: unless-stopped
    depends_on:
      keycloak:
        condition: service_healthy
      quarkus-probabilidad:
        condition: service_started
    healthcheck:
      test: ["CMD-SHELL", "wget --no-verbose --tries=1 --spider http://localhost/health || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
    networks:
      - app-network
```

Luego ejecuta desde el directorio `infra/`:

```bash
cd ../infra
docker-compose up -d
```

## Verificación

### Health Check

```bash
# Verificar estado del contenedor
docker ps

# Verificar health endpoint
curl http://localhost:3000/health
```

### Logs

```bash
# Ver logs en tiempo real
docker logs -f frontend-probabilidad

# Ver últimas 100 líneas
docker logs --tail 100 frontend-probabilidad
```

## Comandos Útiles

```bash
# Detener el contenedor
docker-compose down

# Reconstruir la imagen
docker-compose build --no-cache

# Ver uso de recursos
docker stats frontend-probabilidad

# Ejecutar shell dentro del contenedor
docker exec -it frontend-probabilidad sh

# Limpiar imágenes antiguas
docker image prune -f
```

## Optimizaciones Implementadas

### Dockerfile
- Build multi-stage para reducir el tamaño final de la imagen
- Uso de `node:20-alpine` para menor tamaño
- Uso de `pnpm` con `--frozen-lockfile` para builds reproducibles
- Variables de entorno inyectadas en tiempo de build
- Health check integrado

### Nginx
- Compresión gzip habilitada
- Caché de assets estáticos (1 año)
- Headers de seguridad (X-Frame-Options, X-Content-Type-Options, etc.)
- Fallback para SPAs (todas las rutas → index.html)
- Endpoint de health check en `/health`

### .dockerignore
- Excluye `node_modules`, archivos de desarrollo y documentación
- Reduce significativamente el contexto de build
- Mejora la velocidad de construcción

## Troubleshooting

### Error: Cannot find module

Si ves errores de módulos faltantes, verifica que `pnpm-lock.yaml` esté actualizado:

```bash
pnpm install
docker-compose build --no-cache
```

### Error: VITE environment variables not defined

Asegúrate de pasar las variables como build args o define un archivo `.env`:

```bash
docker build --build-arg VITE_KEYCLOAK_URL=... -t frontend-probabilidad .
```

### Error 404 en rutas de React Router

Esto significa que nginx no está configurado correctamente. Verifica que `nginx.conf` se copie correctamente en el Dockerfile.

### Puerto 3000 ya en uso

Cambia el puerto en `docker-compose.yml`:

```yaml
ports:
  - "3001:80"  # Usar puerto 3001 en lugar de 3000
```

## Variables de Entorno

| Variable | Descripción | Ejemplo |
|----------|-------------|---------|
| `VITE_KEYCLOAK_URL` | URL del servidor Keycloak | `http://localhost:8080` |
| `VITE_KEYCLOAK_REALM` | Realm de Keycloak | `Probabilidad` |
| `VITE_KEYCLOAK_CLIENT_ID` | Client ID en Keycloak | `frontend-spa` |
| `VITE_API_BASE` | URL base del API | `http://localhost:8082/api` |

## Tamaño de la Imagen

La imagen final debería tener aproximadamente:
- Imagen de build: ~800MB (descartada)
- Imagen final (nginx): ~50-60MB

Verifica con:
```bash
docker images frontend-probabilidad
```
