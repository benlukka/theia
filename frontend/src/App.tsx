import React, { useState, useEffect } from 'react';
import './App.css';
import type { LayoutUpdate, TextComponent as TextComponentType, ChartComponent as ChartComponentType } from './generated/index';
import { DefaultApi } from './generated';
import { TextComponent } from './components/TextComponent';
import { ChartComponent } from './components/ChartComponent';
import * as Sentry from "@sentry/react";
function App() {
    const [layout, setLayout] = useState<LayoutUpdate | null>(null);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const api = new DefaultApi();
        api.getLayout().then(response => {
            setLayout(response);
        }).catch(error => {
            const errorMessage = error instanceof Error ? error.message : String(error);
            setError(errorMessage);
            console.error(error);
        });
    }, []);

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
                    {layout?.components?.map((component) => {
                        if (component.type === "text") {
                            return (
                                <TextComponent
                                    key={component.id}
                                    component={component as TextComponentType}
                                />
                            );
                        }
                        if (component.type === "chart") {
                            const chart = component as ChartComponentType;
                            // Optionally, use chart.data.title or chart.id as title
                            return (
                                <ChartComponent
                                    key={chart.id}
                                    chart={chart}
                                    title={chart.id}
                                />
                            );
                        }
                        return null;
                    })}
                </div>
            </header>
        </div>
    );
}

export default App;