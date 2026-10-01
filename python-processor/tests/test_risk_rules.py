from app.rules.risk_rules import evaluate_risk
from app.models.incident import Incident

def create_mock_incident(severity="HIGH", category="TIMEOUT", retryCount=0, status="OPEN"):
    return Incident(
        id=1, incidentKey="INC-001", serviceName="test", resourceId="1",
        category=category, severity=severity, status=status, title="test",
        description="test", errorCode=None, errorMessage=None, retryCount=retryCount,
        resolutionNotes=None, createdAt="now", updatedAt="now", resolvedAt=None
    )

def test_critical_not_eligible():
    incident = create_mock_incident(severity="CRITICAL")
    risk, eligible, reason = evaluate_risk(incident)
    assert risk == "CRITICAL"
    assert eligible is False

def test_high_service_failure_eligible():
    incident = create_mock_incident(severity="HIGH", category="FULFILLMENT_FAILURE")
    risk, eligible, reason = evaluate_risk(incident)
    assert risk == "HIGH"
    assert eligible is True

def test_medium_eligible():
    incident = create_mock_incident(severity="MEDIUM")
    risk, eligible, reason = evaluate_risk(incident)
    assert risk == "MEDIUM"
    assert eligible is True

def test_low_not_eligible():
    incident = create_mock_incident(severity="LOW")
    risk, eligible, reason = evaluate_risk(incident)
    assert risk == "LOW"
    assert eligible is False

def test_retry_count_exceeded():
    incident = create_mock_incident(severity="HIGH", category="TIMEOUT", retryCount=3)
    risk, eligible, reason = evaluate_risk(incident, max_attempts=3)
    assert eligible is False
    assert "Maximum remediation attempts reached" in reason

def test_non_open_not_eligible():
    incident = create_mock_incident(status="INVESTIGATING")
    risk, eligible, reason = evaluate_risk(incident)
    assert eligible is False
