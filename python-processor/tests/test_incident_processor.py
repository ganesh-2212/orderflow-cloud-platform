import pytest
from unittest.mock import Mock
from app.processor.incident_processor import IncidentProcessor
from app.models.incident import Incident

def create_mock_incident(id=1, severity="HIGH", category="TIMEOUT", retryCount=0):
    return Incident(
        id=id, incidentKey=f"INC-{id}", serviceName="test", resourceId=str(id),
        category=category, severity=severity, status="OPEN", title="test",
        description="test", errorCode=None, errorMessage=None, retryCount=retryCount,
        resolutionNotes=None, createdAt="now", updatedAt="now", resolvedAt=None
    )

def test_process_empty_list():
    mock_client = Mock()
    mock_client.get_open_incidents.return_value = []
    
    processor = IncidentProcessor(mock_client)
    results = processor.process_open_incidents()
    assert len(results) == 0

def test_dry_run_does_not_call_remediation():
    mock_client = Mock()
    incident = create_mock_incident()
    mock_client.get_open_incidents.return_value = [incident]
    
    processor = IncidentProcessor(mock_client, dry_run=True)
    results = processor.process_open_incidents()
    
    assert len(results) == 1
    assert results[0].remediation_eligible is True
    assert results[0].remediation_attempted is False
    mock_client.trigger_remediation.assert_not_called()

def test_live_mode_calls_remediation():
    mock_client = Mock()
    incident = create_mock_incident()
    mock_client.get_open_incidents.return_value = [incident]
    mock_client.trigger_remediation.return_value = True
    
    processor = IncidentProcessor(mock_client, dry_run=False)
    results = processor.process_open_incidents()
    
    assert len(results) == 1
    assert results[0].remediation_attempted is True
    assert results[0].remediation_successful is True
    mock_client.trigger_remediation.assert_called_once_with(1)

def test_failed_incident_does_not_stop_batch():
    mock_client = Mock()
    inc1 = create_mock_incident(id=1)
    inc2 = create_mock_incident(id=2)
    
    # Simulating a failure during processing of inc1 
    # (By mocking normalize to throw for inc1)
    mock_client.get_open_incidents.return_value = [inc1, inc2]
    
    processor = IncidentProcessor(mock_client, dry_run=False)
    # Monkey-patch normalize_incident to throw on id 1
    original_normalize = processor.normalize_incident
    def mock_normalize(inc):
        if inc.id == 1:
            raise ValueError("Malformed")
        return original_normalize(inc)
    processor.normalize_incident = mock_normalize
    
    results = processor.process_open_incidents()
    
    assert len(results) == 1  # only inc2 succeeds
    assert results[0].incident_id == 2
