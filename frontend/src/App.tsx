import React, { useState, useEffect } from 'react';
import './App.css';
import type { LayoutUpdate, TextComponent } from '../generated';

const TextComponentDisplay = ({ component }: { component: TextComponent }) => (
    <div className="text-component" style={{
      padding: '10px',
      margin: '5px',
      border: '1px solid #ccc',
      borderRadius: '4px'
    }}>
      <label>
        <span style={{ fontWeight: 'bold' }}>ID: </span>{component.id}
      </label>
      <p>{component.text}</p>
    </div>
);

function App() {
  const [layout, setLayout] = useState<LayoutUpdate | null>(null);
  const [error, setError] = useState<string | null>(null);

  const API_URL = process.env.NODE_ENV === 'development'
      ? 'http://localhost:8080/api'
      : '/api';

  useEffect(() => {
    const fetchLayoutUpdate = async () => {
      try {
        const response = await fetch(`${API_URL}/layout-update`, {
          method: 'GET',
          headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json'
          },
          mode: 'cors'
        });

        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }

        const data = await response.json();
        setLayout(data);
      } catch (error) {
        setError(error instanceof Error ? error.message : 'Failed to fetch layout');
      }
    };

    fetchLayoutUpdate();
  }, [API_URL]);

  return (
      <div className="App">
        <header className="App-header">
          {error && (
              <div style={{ color: '#ff4444', padding: '10px', margin: '10px 0' }}>
                Error: {error}
              </div>
          )}
          <div className="layout-container" style={{
            display: 'flex',
            flexDirection: 'column',
            gap: '10px',
            padding: '20px'
          }}>
            {layout?.components?.map((component) => (
                'text' in component ? (
                    <TextComponentDisplay
                        key={component.id}
                        component={component as TextComponent}
                    />
                ) : null
            ))}
          </div>
        </header>
      </div>
  );
}

export default App;