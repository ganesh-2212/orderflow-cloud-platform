import os
import sys
import logging
from app.client.incident_client import IncidentClient
from app.processor.incident_processor import IncidentProcessor
from app.utils.logging_config import setup_logging

def main():
    setup_logging()
    logger = logging.getLogger(__name__)

    base_url = os.environ.get("INCIDENT_SERVICE_URL")
    if not base_url:
        logger.error("Configuration error: INCIDENT_SERVICE_URL environment variable is required")
        sys.exit(1)

    timeout = int(os.environ.get("PROCESSOR_TIMEOUT_SECONDS", "5"))
    batch_size = int(os.environ.get("PROCESSOR_BATCH_SIZE", "50"))
    max_attempts = int(os.environ.get("MAX_REMEDIATION_ATTEMPTS", "3"))
    
    dry_run_env = os.environ.get("PROCESSOR_DRY_RUN", "true").lower()
    dry_run = dry_run_env in ("true", "1", "yes")

    logger.info(f"Starting Python Processor with INCIDENT_SERVICE_URL={base_url}")
    logger.info(f"Configuration: dry_run={dry_run}, batch_size={batch_size}, max_attempts={max_attempts}")

    client = IncidentClient(base_url, timeout)
    processor = IncidentProcessor(client, dry_run=dry_run, batch_size=batch_size, max_attempts=max_attempts)

    try:
        results = processor.process_open_incidents()
        
        # Check if any remediation failed (just as an example of checking recoverable failures)
        # Not terminating if one incident fails, but we might want to return exit code 2
        # if there were errors processing some incidents that we attempted to remediate.
        failed_remediations = sum(1 for r in results if r.remediation_attempted and not r.remediation_successful)
        
        if failed_remediations > 0:
            logger.warning(f"Exiting with status 2: Encountered {failed_remediations} failed remediations.")
            sys.exit(2)
            
        sys.exit(0)
    except Exception as e:
        logger.error(f"Fatal error during processing: {str(e)}")
        sys.exit(1)

if __name__ == "__main__":
    main()
