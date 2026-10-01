import React from 'react';

interface HeaderProps {
  lastUpdated: string;
  isUpdating: boolean;
}

export const Header: React.FC<HeaderProps> = ({ lastUpdated, isUpdating }) => {
  return (
    <header className="dashboard-header">
      <div className="title-section">
        <h1>OrderFlow Operations Dashboard</h1>
        <div className={`status-indicator ${isUpdating ? 'updating' : 'online'}`}>
          {isUpdating ? 'Refreshing...' : 'System Online'}
        </div>
      </div>
      <div className="time-section">
        Last updated: {lastUpdated}
      </div>
    </header>
  );
};
