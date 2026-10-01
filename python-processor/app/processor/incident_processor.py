import logging
import os
from typing import List
from app.client.incident_client import IncidentClient, IncidentClientError
from app.models.incident import Incident
from app.processor.result import ProcessingResult
from app.rules.risk_rules import evaluate_risk

logger = logging.getLogger(__name__)

class IncidentProcessor:
    def __init__(self, client: IncidentClient, dry_run: bool = True, batch_size: int = 50, max_attempts: int = 3):
        self.client = client
        self.dry_run = dry_run
        self.batch_size = batch_size
        self.max_attempts = max_attempts

    def normalize_incident(self, incident: Incident) -> Incident:
        incident.serviceName = incident.serviceName.lower().strip()
        incident.category = incident.category.upper().strip()
        incident.severity = incident.severity.upper().strip()
        incident.status = incident.status.upper().strip()
        if incident.errorCode:
            incident.errorCode = incident.errorCode.upper().strip()
        return incident

    def process_open_incidents(self) -> List[ProcessingResult]:
        logger.info("Processor started.")
        results = []
        try:
            incidents = self.client.get_open_incidents()
        except IncidentClientError as e:
            logger.error("Failed to retrieve incidents, aborting batch.")
            return results

        logger.info(f"Fetched {len(incidents)} open incidents")
        
        # Respect batch size
        incidents_to_process = incidents[:self.batch_size]
        
        for incident in incidents_to_process:
            logger.info(f"Processing incident key={incident.incidentKey}")
            try:
                normalized = self.normalize_incident(incident)
                risk_level, eligible, reason = evaluate_risk(normalized, self.max_attempts)
                
                logger.info(f"Incident {normalized.incidentKey} classified as {risk_level}")
                
                attempted = False
                successful = False

                if eligible:
                    if self.dry_run:
                        logger.info(f"DRY RUN: incident {normalized.incidentKey} would be remediated. Reason: {reason}")
                    else:
                        logger.info(f"Attempting remediation for {normalized.incidentKey}")
                        attempted = True
                        try:
                            successful = self.client.trigger_remediation(normalized.id)
                            if successful:
                                logger.info(f"Remediation successful: incident={normalized.incidentKey}")
                            else:
                                logger.info(f"Remediation failed: incident={normalized.incidentKey}")
                        except IncidentClientError as ex:
                            logger.error(f"Error during remediation of {normalized.incidentKey}: {str(ex)}")
                            successful = False
                else:
                    logger.info(f"Incident {normalized.incidentKey} skipped: {reason}")
                
                results.append(ProcessingResult(
                    incident_id=normalized.id,
                    incident_key=normalized.incidentKey,
                    risk_level=risk_level,
                    remediation_eligible=eligible,
                    remediation_attempted=attempted,
                    remediation_successful=successful,
                    reason=reason
                ))
            except Exception as e:
                logger.error(f"Malformed incident or unexpected error processing {incident.incidentKey}: {str(e)}")
                continue

        # Summary
        processed = len(results)
        eligible_count = sum(1 for r in results if r.remediation_eligible)
        remediated_count = sum(1 for r in results if r.remediation_attempted)
        success_count = sum(1 for r in results if r.remediation_successful)
        skipped = processed - eligible_count
        
        logger.info(f"Processing completed: processed={processed} eligible={eligible_count} remediated={remediated_count} successful={success_count} skipped={skipped}")
        return results
