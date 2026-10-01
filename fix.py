import pathlib
p = pathlib.Path('docker-compose.yml')
c = p.read_text(encoding='utf-8')
c = c.replace('      REPLACE_ME:\n        - VITE_ORDER_SERVICE_URL=\n        - VITE_INVENTORY_SERVICE_URL=\n        - VITE_FULFILLMENT_SERVICE_URL=\n        - VITE_INCIDENT_SERVICE_URL=', '      args:\n        - VITE_ORDER_SERVICE_URL=${VITE_ORDER_SERVICE_URL:-http://localhost:8081}\n        - VITE_INVENTORY_SERVICE_URL=${VITE_INVENTORY_SERVICE_URL:-http://localhost:8082}\n        - VITE_FULFILLMENT_SERVICE_URL=${VITE_FULFILLMENT_SERVICE_URL:-http://localhost:8083}\n        - VITE_INCIDENT_SERVICE_URL=${VITE_INCIDENT_SERVICE_URL:-http://localhost:8084}')
p.write_text(c, encoding='utf-8')
