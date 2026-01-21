import React, { useState, useEffect } from 'react';
import { Watch, Moon, Activity, Timer, TrendingUp, AlertCircle, RefreshCw } from 'lucide-react';
import './Smartwatch.css';

const BACKEND_URL =
  process.env.REACT_APP_BACKEND?.includes("/api/connect")
    ? process.env.REACT_APP_BACKEND.replace("/api/connect", "")
    : process.env.REACT_APP_BACKEND || "http://localhost:8080";


const SmartwatchTab = ({ getAccessTokenSilently, isAuthenticated }) => {
  const [integrationStatus, setIntegrationStatus] = useState(null);
  const [loading, setLoading] = useState(true);
  const [connectingTerra, setConnectingTerra] = useState(false);
  const [sleepData, setSleepData] = useState(null);
  const [error, setError] = useState(null);
  const [refreshing, setRefreshing] = useState(false);

  useEffect(() => {
    checkConnection();
  }, []);

  const getAuthHeaders = async () => {
    const localToken = localStorage.getItem("token");
    
    // Auth0 login (Google, etc.)
    if (isAuthenticated && getAccessTokenSilently) {
      const token = await getAccessTokenSilently();
      return {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      };
    }
    // Local JWT login
    else if (localToken) {
      return {
        'Authorization': `Bearer ${localToken}`,
        'Content-Type': 'application/json'
      };
    }
    
    throw new Error('Not authenticated');
  };

  const checkConnection = async () => {
    try {
      const headers = await getAuthHeaders();
      const res = await fetch(`${BACKEND_URL}/api/sleep/integrations/me/TERRA`, { headers });
      
      console.log('Connection check response status:', res.status);
      
      if (res.ok) {
        const contentType = res.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
          const status = await res.json();
          console.log('Integration status:', status);
          setIntegrationStatus(status);
          if (status.connected) {
            fetchSleepData();
          }
        } else {
          const text = await res.text();
          console.error('Expected JSON but got:', text.substring(0, 100));
          setError('Server vratio HTML umjesto JSON podataka. Provjerite API rutu.');
          setIntegrationStatus({ provider: 'TERRA', connected: false });
        }
      } else if (res.status === 404) {
        console.log('No integration found (404) - user not connected');
        setIntegrationStatus({ provider: 'TERRA', connected: false });
        setError(null);
      } else {
        const contentType = res.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
          const errorData = await res.json();
          console.error('Error response:', errorData);
          setError(`Greška ${res.status}: ${errorData.message || 'Nepoznata greška'}`);
        } else {
          const errorText = await res.text();
          console.error('Error response (non-JSON):', errorText.substring(0, 200));
          setError(`Greška ${res.status}: Server nije vratio JSON odgovor`);
        }
        setIntegrationStatus({ provider: 'TERRA', connected: false });
      }
    } catch (error) {
      console.error('Network or fetch error:', error);
      setError(`Greška pri povezivanju: ${error.message}`);
      setIntegrationStatus({ provider: 'TERRA', connected: false });
    } finally {
      setLoading(false);
    }
  };

  const fetchSleepData = async () => {
    try {
      setRefreshing(true);
      const headers = await getAuthHeaders();
      const res = await fetch(`${BACKEND_URL}/api/sleep/summary/me?provider=TERRA`, { headers });
      
      if (!res.ok) {
        throw new Error('Failed to fetch sleep data');
      }
      
      const contentType = res.headers.get('content-type');
      if (contentType && contentType.includes('application/json')) {
        const data = await res.json();
        console.log('Sleep data received:', data);
        setSleepData(data);
        setError(null);
      } else {
        throw new Error('Server nije vratio JSON podatke');
      }
    } catch (error) {
      console.error('Error fetching sleep data:', error);
      setError('Greška pri učitavanju podataka o snu');
    } finally {
      setRefreshing(false);
    }
  };

  const handleConnect = async () => {
    setConnectingTerra(true);
    setError(null);
    try {
      const headers = await getAuthHeaders();
      const res = await fetch(`${BACKEND_URL}/api/sleep/terra/widget-session`, { headers });
      
      console.log('Widget session response status:', res.status);
      console.log('Widget session response headers:', res.headers.get('content-type'));
      
      if (!res.ok) {
        const errorText = await res.text();
        console.error('Widget session error response:', errorText);
        throw new Error(`Server error: ${res.status}`);
      }
      
      const contentType = res.headers.get('content-type');
      if (!contentType || !contentType.includes('application/json')) {
        const responseText = await res.text();
        console.error('Expected JSON but got:', responseText.substring(0, 200));
        throw new Error('Server nije vratio JSON odgovor. Provjerite backend konfiguraciju.');
      }
      
      const data = await res.json();
      console.log('Widget session data:', data);
      
      if (!data.url) {
        throw new Error('Terra nije vratio widget URL');
      }
      
      const width = 500;
      const height = 700;
      const left = window.screen.width / 2 - width / 2;
      const top = window.screen.height / 2 - height / 2;
      
      const popup = window.open(
        data.url,
        'TerraConnect',
        `width=${width},height=${height},left=${left},top=${top}`
      );

      if (!popup) {
        throw new Error('Nije moguće otvoriti popup. Provjerite browser postavke.');
      }

      const checkInterval = setInterval(async () => {
        try {
          const checkHeaders = await getAuthHeaders();
          const checkRes = await fetch(`${BACKEND_URL}/api/sleep/integrations/me/TERRA`, { headers: checkHeaders });
          
          if (checkRes.ok) {
            const checkContentType = checkRes.headers.get('content-type');
            if (checkContentType && checkContentType.includes('application/json')) {
              const status = await checkRes.json();
              if (status.connected) {
                setIntegrationStatus(status);
                clearInterval(checkInterval);
                if (popup && !popup.closed) popup.close();
                fetchSleepData();
                setConnectingTerra(false);
              }
            }
          }
        } catch (error) {
          console.error('Error checking connection status:', error);
        }
      }, 2000);

      setTimeout(() => {
        clearInterval(checkInterval);
        setConnectingTerra(false);
      }, 300000);
    } catch (error) {
      console.error('Error connecting to Terra:', error);
      setError(`Greška pri povezivanju: ${error.message}`);
      setConnectingTerra(false);
    }
  };

  const formatDate = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('hr-HR', { 
      day: 'numeric', 
      month: 'short', 
      year: 'numeric' 
    });
  };

  const formatDateTime = (dateTimeString) => {
    if (!dateTimeString) return '';
    const date = new Date(dateTimeString);
    return date.toLocaleString('hr-HR', { 
      day: 'numeric', 
      month: 'short', 
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const getScoreColor = (score) => {
    if (score >= 80) return 'score-excellent';
    if (score >= 60) return 'score-good';
    return 'score-poor';
  };

  const getScoreLabel = (score) => {
    if (score >= 80) return 'Odličan';
    if (score >= 60) return 'Dobar';
    return 'Loš';
  };

  if (loading) {
    return (
      <div className="smartwatch-container">
        <div className="loading-text">Učitavanje...</div>
      </div>
    );
  }

  if (!integrationStatus || !integrationStatus.connected) {
    return (
      <div className="smartwatch-container">
        <div className="smartwatch-content-narrow">
          <div className="onboarding-header">
            <div className="watch-icon-wrapper">
              <Watch className="watch-icon" />
              <div className="watch-badge">
                <Moon className="moon-icon" />
              </div>
            </div>
            <h1 className="onboarding-title">Poveži svoj Smartwatch</h1>
            <p className="onboarding-subtitle">
              Prati kvalitetu sna i dobij personalizirane uvide sa svog Terra kompatibilnog uređaja
            </p>
          </div>

          {error && (
            <div className="error-box">
              <AlertCircle className="error-icon" />
              <p className="error-text">{error}</p>
            </div>
          )}

          <div className="features-card">
            <h3 className="features-title">Što ćeš dobiti?</h3>
            
            <div className="features-list">
              <div className="feature-item">
                <div className="feature-icon-box feature-icon-purple">
                  <Moon className="feature-icon" />
                </div>
                <div>
                  <h3 className="feature-name">Sleep Score (0-100)</h3>
                  <p className="feature-description">Sveobuhvatna procjena kvalitete tvog sna zasnovana na više faktora</p>
                </div>
              </div>
              
              <div className="feature-item">
                <div className="feature-icon-box feature-icon-blue">
                  <Timer className="feature-icon" />
                </div>
                <div>
                  <h3 className="feature-name">Sleep Latency</h3>
                  <p className="feature-description">Koliko dugo ti treba da zaspiš nakon što legneš u krevet</p>
                </div>
              </div>
              
              <div className="feature-item">
                <div className="feature-icon-box feature-icon-yellow">
                  <Activity className="feature-icon" />
                </div>
                <div>
                  <h3 className="feature-name">Buđenja</h3>
                  <p className="feature-description">Broj puta koliko si se probudio tijekom noći</p>
                </div>
              </div>
              
              <div className="feature-item">
                <div className="feature-icon-box feature-icon-green">
                  <TrendingUp className="feature-icon" />
                </div>
                <div>
                  <h3 className="feature-name">Sleep Efficiency</h3>
                  <p className="feature-description">Postotak vremena provedenog u krevetu stvarno spavajući</p>
                </div>
              </div>
            </div>

            <button
              onClick={handleConnect}
              disabled={connectingTerra}
              className="connect-button"
            >
              <Watch className="button-icon" />
              {connectingTerra ? 'Povezivanje...' : 'Poveži uređaj'}
            </button>
            
            <p className="supported-devices">
              Podržani uređaji: Apple Watch, Fitbit, Garmin, Oura, Whoop i drugi
            </p>
          </div>
        </div>
      </div>
    );
  }

  const latestScore = sleepData?.latest ?? null;
  const last7Days = Array.isArray(sleepData?.last7Days) ? sleepData.last7Days.slice(0, 7) : [];
  const averageScore = last7Days.length > 0 
    ? Math.round(last7Days.reduce((sum, day) => sum + (day.score || 0), 0) / last7Days.length)
    : 0;

  return (
    <div className="smartwatch-container">
      <div className="smartwatch-content-wide">
        <div className="header-section">
          <div>
            <h1 className="page-title">
              <Moon className="title-icon" />
              Sleep Analytics
            </h1>
            <p className="page-subtitle">Tvoji sleep podatci sa Terra uređaja</p>
          </div>
          <div className="header-actions">
            {integrationStatus.connected && (
              <button
                onClick={fetchSleepData}
                disabled={refreshing}
                className="refresh-button"
              >
                <RefreshCw className={`refresh-icon ${refreshing ? 'spinning' : ''}`} />
                Osvježi
              </button>
            )}
            <div className="status-section">
              <div className="status-badge">
                <div className="status-dot"></div>
                Povezano
              </div>
              {integrationStatus?.connectedAt && (
                <span className="status-date">
                  {formatDateTime(integrationStatus.connectedAt)}
                </span>
              )}
            </div>
          </div>
        </div>

        {error && (
          <div className="error-box">
            <AlertCircle className="error-icon" />
            <p className="error-text">{error}</p>
          </div>
        )}

        {!last7Days || last7Days.length === 0 ? (
          <div className="empty-state">
            <Moon className="empty-icon" />
            <p className="empty-title">Nema dostupnih podataka</p>
            <p className="empty-subtitle">Podatci će se pojaviti nakon prvog sinkroniziranog sna</p>
          </div>
        ) : (
          <div className="data-section">
            {latestScore && (
              <div className="score-card">
                <div className="score-header">
                  <h2 className="section-title">Zadnji Sleep Score</h2>
                  <span className="score-date">{formatDate(latestScore.date)}</span>
                </div>
                
                <div className="score-display">
                  <div className={`score-circle ${getScoreColor(latestScore.score)}`}>
                    <div className="score-circle-inner">
                      <div className="score-number">{latestScore.score}</div>
                      <div className="score-label">{getScoreLabel(latestScore.score)}</div>
                    </div>
                  </div>
                </div>

                <div className="metrics-grid">
                  {latestScore.latencyMinutes !== null && (
                    <div className="metric-card">
                      <div className="metric-header">
                        <Timer className="metric-icon metric-icon-blue" />
                        <span className="metric-label">Sleep Latency</span>
                      </div>
                      <div className="metric-value">{latestScore.latencyMinutes} min</div>
                      <div className="metric-description">Vrijeme do uspavljivanja</div>
                    </div>
                  )}
                  
                  {latestScore.awakeningsCount !== null && (
                    <div className="metric-card">
                      <div className="metric-header">
                        <Activity className="metric-icon metric-icon-yellow" />
                        <span className="metric-label">Buđenja</span>
                      </div>
                      <div className="metric-value">{latestScore.awakeningsCount}x</div>
                      <div className="metric-description">Broj buđenja tijekom noći</div>
                    </div>
                  )}
                  
                  {latestScore.continuityScore !== null && (
                    <div className="metric-card">
                      <div className="metric-header">
                        <TrendingUp className="metric-icon metric-icon-green" />
                        <span className="metric-label">Sleep Efficiency</span>
                      </div>
                      <div className="metric-value">{latestScore.continuityScore}%</div>
                      <div className="metric-description">Efikasnost sna</div>
                    </div>
                  )}
                </div>
              </div>
            )}

            {last7Days.length > 0 && (
              <div className="history-card">
                <div className="history-header">
                  <h2 className="section-title">Zadnjih 7 dana</h2>
                  <div className="average-score">
                    <div className={`average-number ${getScoreColor(averageScore)}`}>{averageScore}</div>
                    <div className="average-label">Prosječni score</div>
                  </div>
                </div>
                
                <div className="history-list">
                  {last7Days.map((day, idx) => (
                    <div key={idx} className="history-item">
                      <div className="history-content">
                        <div className="history-date">{formatDate(day.date)}</div>
                        <div className="history-metrics">
                          {day.latencyMinutes !== null && (
                            <span className="history-metric">
                              <Timer className="history-metric-icon" />
                              {day.latencyMinutes} min
                            </span>
                          )}
                          {day.awakeningsCount !== null && (
                            <span className="history-metric">
                              <Activity className="history-metric-icon" />
                              {day.awakeningsCount}x buđenja
                            </span>
                          )}
                          {day.continuityScore !== null && (
                            <span className="history-metric">
                              <TrendingUp className="history-metric-icon" />
                              {day.continuityScore}% efficiency
                            </span>
                          )}
                        </div>
                      </div>
                      <div className="history-score">
                        <div className={`history-score-number ${getScoreColor(day.score)}`}>
                          {day.score}
                        </div>
                        <div className={`history-score-label ${getScoreColor(day.score)}`}>
                          {getScoreLabel(day.score)}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default SmartwatchTab;