/* tslint:disable */
/* eslint-disable */
/**
 * Layout Dashboard API
 * API for managing dynamic dashboard layouts and components
 *
 * The version of the OpenAPI document: 1.0.0
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

import { exists, mapValues } from '../runtime';
import {
     AnimationComponentFromJSONTyped,
     ChartComponentFromJSONTyped,
     TextComponentFromJSONTyped
} from './';

/**
 * 
 * @export
 * @interface UIComponent
 */
export interface UIComponent {
    /**
     * 
     * @type {string}
     * @memberof UIComponent
     */
    id: string;
    /**
     * 
     * @type {string}
     * @memberof UIComponent
     */
    type: UIComponentTypeEnum;
}


/**
 * @export
 */
export const UIComponentTypeEnum = {
    CHART: 'chart',
    TEXT: 'text',
    ANIMATION: 'animation'
} as const;
export type UIComponentTypeEnum = typeof UIComponentTypeEnum[keyof typeof UIComponentTypeEnum];


/**
 * Check if a given object implements the UIComponent interface.
 */
export function instanceOfUIComponent(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "id" in value;
    isInstance = isInstance && "type" in value;

    return isInstance;
}

export function UIComponentFromJSON(json: any): UIComponent {
    return UIComponentFromJSONTyped(json, false);
}

export function UIComponentFromJSONTyped(json: any, ignoreDiscriminator: boolean): UIComponent {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    if (!ignoreDiscriminator) {
        if (json['type'] === 'animation') {
            return AnimationComponentFromJSONTyped(json, true);
        }
        if (json['type'] === 'chart') {
            return ChartComponentFromJSONTyped(json, true);
        }
        if (json['type'] === 'text') {
            return TextComponentFromJSONTyped(json, true);
        }
    }
    return {
        
        'id': json['id'],
        'type': json['type'],
    };
}

export function UIComponentToJSON(value?: UIComponent | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'id': value.id,
        'type': value.type,
    };
}

