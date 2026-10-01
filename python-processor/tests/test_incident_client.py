import pytest
import responses
from app.client.incident_client import IncidentClient, IncidentClientError
from app.models.incident import Incident

@responses.activate
def test_get_open_incidents_success():
    client = IncidentClient("http://localhost:8084")
    
    mock_data = [
        {"id": 1, "incidentKey": "INC-001", "serviceName": "order-service", "resourceId": "ORDER-1", "category": "TIMEOUT", "severity": "MEDIUM", "status": "OPEN", "title": "Test", "description": "Test", "retryCount": 0, "createdAt": "now", "updatedAt": "now"}
    ]
    
    responses.add(
        responses.GET,
        "http://localhost:8084/api/v1/incidents/status/OPEN",
        json=mock_data,
        status=200
    )
    
    incidents = client.get_open_incidents()
    assert len(incidents) == 1
    assert incidents[0].incidentKey == "INC-001"

@responses.activate
def test_get_open_incidents_timeout():
    client = IncidentClient("http://localhost:8084")
    import requests
    responses.add(
        responses.GET,
        "http://localhost:8084/api/v1/incidents/status/OPEN",
        body=requests.exceptions.Timeout("Connection timed out")
    )
    
    with pytest.raises(IncidentClientError):
        client.get_open_incidents()

@responses.activate
def test_remediation_success():
    client = IncidentClient("http://localhost:8084")
    responses.add(
        responses.POST,
        "http://localhost:8084/api/v1/incidents/1/remediation",
        json={"status": "MITIGATING"},
        status=200
    )
    
    success = client.trigger_remediation(1)
    assert success is True

@responses.activate
def test_remediation_409():
    client = IncidentClient("http://localhost:8084")
    responses.add(
        responses.POST,
        "http://localhost:8084/api/v1/incidents/1/remediation",
        status=409
    )
    
    success = client.trigger_remediation(1)
    assert success is False
