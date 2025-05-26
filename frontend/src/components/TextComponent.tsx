import type {TextComponent as TextComponentType} from "../generated";
import React from "react";

export const TextComponent: React.FC<{ component: TextComponentType }> = ({ component }) => (
    <div className="text-component" style={{
        padding: '10px',
        margin: '5px',
        border: '1px solid #ccc',
        borderRadius: '4px'
    }}>
        <p>{component.text}</p>
    </div>
);