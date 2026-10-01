# Python Operational Processor

The Python Processor is a lightweight operational automation component. It **does not** own incident data or business transactions. Its sole responsibility is to scan for `OPEN` incidents from the Incident Service, evaluate them using deterministic risk rules, and automatically trigger remediation for eligible incidents.

## Architecture
- **Language**: Python 3.11+
- **Pattern**: Polling execution via one-shot run script
- **Communication**: Synchronous HTTP via `requests`

## Configuration
Use environment variables to configure the processor:
- `INCIDENT_SERVICE_URL`: Base URL of the incident service (e.g. `http://localhost:8084`)
- `PROCESSOR_TIMEOUT_SECONDS`: HTTP timeout duration (default: 5)
- `PROCESSOR_BATCH_SIZE`: Maximum number of incidents to evaluate per cycle (default: 50)
- `MAX_REMEDIATION_ATTEMPTS`: Absolute limit of retries (default: 3)
- `PROCESSOR_DRY_RUN`: Controls whether to mutate data (`true`/`false`). Default is `true`.

## Dry-run Mode
When `PROCESSOR_DRY_RUN=true`, the processor evaluates incidents and logs the proposed actions, but skips calling the actual remediation HTTP POST endpoint.

## Execution
Run a single polling cycle:
```bash
# Provide the environment variable
INCIDENT_SERVICE_URL=http://localhost:8084 python -m app.main
```

Using Docker:
```bash
docker build -t orderflow-python-processor ./python-processor
docker run --rm \
  -e INCIDENT_SERVICE_URL=http://host.docker.internal:8084 \
  -e PROCESSOR_DRY_RUN=true \
  orderflow-python-processor
```

## Testing
To run the automated tests via `pytest`:
```bash
pip install -r requirements.txt
pytest
```

## Example Output
```text
2026-09-28 12:00:00 - app.main - INFO - Starting Python Processor with INCIDENT_SERVICE_URL=http://localhost:8084
2026-09-28 12:00:00 - app.processor.incident_processor - INFO - Fetched 8 open incidents
2026-09-28 12:00:00 - app.processor.incident_processor - INFO - Incident INC-001 classified as HIGH
2026-09-28 12:00:00 - app.processor.incident_processor - INFO - DRY RUN: incident INC-001 would be remediated. Reason: High-severity FULFILLMENT_FAILURE eligible for bounded retry
2026-09-28 12:00:00 - app.processor.incident_processor - INFO - Processing completed: processed=8 eligible=3 remediated=0 successful=0 skipped=5
```
