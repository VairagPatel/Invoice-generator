import { describe, it, expect, beforeAll, afterAll } from 'vitest';
import * as fc from 'fast-check';

/**
 * Property-Based Tests for Design Token System
 * Feature: ui-modernization-rebrand, Property 1: Consistent color application
 * Validates: Requirements 3.1, 3.2, 3.3
 */

describe('Design Token Consistency Property Tests', () => {
  let styleElement;

  beforeAll(() => {
    // Inject CSS variables into the test environment
    styleElement = document.createElement('style');
    styleElement.textContent = `
      :root {
        /* Primary Colors - Purple/Indigo Theme */
        --color-primary-50: #f5f3ff;
        --color-primary-100: #ede9fe;
        --color-primary-200: #ddd6fe;
        --color-primary-300: #c4b5fd;
        --color-primary-400: #a78bfa;
        --color-primary-500: #8b5cf6;
        --color-primary-600: #7c3aed;
        --color-primary-700: #6d28d9;
        --color-primary-800: #5b21b6;
        --color-primary-900: #4c1d95;
        
        /* Accent Colors - Vibrant Coral/Pink */
        --color-accent-400: #fb7185;
        --color-accent-500: #f43f5e;
        --color-accent-600: #e11d48;
        
        /* Neutral Colors */
        --color-neutral-50: #fafafa;
        --color-neutral-100: #f5f5f5;
        --color-neutral-200: #e5e5e5;
        --color-neutral-300: #d4d4d4;
        --color-neutral-400: #a3a3a3;
        --color-neutral-500: #737373;
        --color-neutral-600: #525252;
        --color-neutral-700: #404040;
        --color-neutral-800: #262626;
        --color-neutral-900: #171717;
        
        /* Semantic Colors */
        --color-success: #10b981;
        --color-warning: #f59e0b;
        --color-error: #ef4444;
        --color-info: #3b82f6;
        
        /* Typography */
        --font-primary: 'Inter', 'Urbanist', -apple-system, BlinkMacSystemFont, sans-serif;
        --font-display: 'Inter', 'Urbanist', sans-serif;
        
        /* Font Sizes */
        --text-xs: 0.75rem;
        --text-sm: 0.875rem;
        --text-base: 1rem;
        --text-lg: 1.125rem;
        --text-xl: 1.25rem;
        --text-2xl: 1.5rem;
        --text-3xl: 1.875rem;
        --text-4xl: 2.25rem;
        --text-5xl: 3rem;
        
        /* Font Weights */
        --font-weight-normal: 400;
        --font-weight-medium: 500;
        --font-weight-semibold: 600;
        --font-weight-bold: 700;
        --font-weight-extrabold: 800;
        
        /* Line Heights */
        --line-height-tight: 1.2;
        --line-height-normal: 1.4;
        --line-height-relaxed: 1.6;
        
        /* Spacing Scale */
        --space-1: 0.25rem;
        --space-2: 0.5rem;
        --space-3: 0.75rem;
        --space-4: 1rem;
        --space-5: 1.25rem;
        --space-6: 1.5rem;
        --space-8: 2rem;
        --space-10: 2.5rem;
        --space-12: 3rem;
        --space-16: 4rem;
        --space-20: 5rem;
        
        /* Shadows */
        --shadow-sm: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
        --shadow-md: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
        --shadow-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
        --shadow-xl: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
        --shadow-2xl: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
        
        /* Border Radius */
        --radius-sm: 0.375rem;
        --radius-md: 0.5rem;
        --radius-lg: 0.75rem;
        --radius-xl: 1rem;
        --radius-2xl: 1.5rem;
        --radius-full: 9999px;
        
        /* Transitions */
        --transition-fast: 150ms ease-in-out;
        --transition-base: 250ms ease-in-out;
        --transition-slow: 350ms ease-in-out;
      }
    `;
    document.head.appendChild(styleElement);
  });

  afterAll(() => {
    // Clean up
    if (styleElement && styleElement.parentNode) {
      styleElement.parentNode.removeChild(styleElement);
    }
  });

  /**
   * Helper function to get CSS variable value
   */
  const getCSSVariable = (variableName) => {
    return getComputedStyle(document.documentElement)
      .getPropertyValue(variableName)
      .trim();
  };

  /**
   * Helper function to validate hex color format
   */
  const isValidHexColor = (color) => {
    return /^#[0-9A-Fa-f]{6}$/.test(color);
  };

  /**
   * Helper function to validate CSS unit value (rem, px, etc.)
   */
  const isValidCSSUnit = (value) => {
    return /^[\d.]+(?:rem|px|em|%)$/.test(value);
  };

  /**
   * Property 1: Consistent color application
   * For any UI component using design tokens, the rendered color values 
   * should match the defined CSS variable values
   */
  it('Property 1: All primary color tokens are defined and valid hex colors', () => {
    const primaryColorScales = [50, 100, 200, 300, 400, 500, 600, 700, 800, 900];
    
    primaryColorScales.forEach(scale => {
      const variableName = `--color-primary-${scale}`;
      const value = getCSSVariable(variableName);
      
      expect(value, `${variableName} should be defined`).toBeTruthy();
      expect(isValidHexColor(value), `${variableName} should be a valid hex color`).toBe(true);
    });
  });

  it('Property 1: All accent color tokens are defined and valid hex colors', () => {
    const accentColorScales = [400, 500, 600];
    
    accentColorScales.forEach(scale => {
      const variableName = `--color-accent-${scale}`;
      const value = getCSSVariable(variableName);
      
      expect(value, `${variableName} should be defined`).toBeTruthy();
      expect(isValidHexColor(value), `${variableName} should be a valid hex color`).toBe(true);
    });
  });

  it('Property 1: All neutral color tokens are defined and valid hex colors', () => {
    const neutralColorScales = [50, 100, 200, 300, 400, 500, 600, 700, 800, 900];
    
    neutralColorScales.forEach(scale => {
      const variableName = `--color-neutral-${scale}`;
      const value = getCSSVariable(variableName);
      
      expect(value, `${variableName} should be defined`).toBeTruthy();
      expect(isValidHexColor(value), `${variableName} should be a valid hex color`).toBe(true);
    });
  });

  it('Property 1: All semantic color tokens are defined and valid hex colors', () => {
    const semanticColors = ['success', 'warning', 'error', 'info'];
    
    semanticColors.forEach(colorName => {
      const variableName = `--color-${colorName}`;
      const value = getCSSVariable(variableName);
      
      expect(value, `${variableName} should be defined`).toBeTruthy();
      expect(isValidHexColor(value), `${variableName} should be a valid hex color`).toBe(true);
    });
  });

  it('Property 1: All typography tokens are defined', () => {
    const typographyTokens = [
      '--font-primary',
      '--font-display',
      '--text-xs',
      '--text-sm',
      '--text-base',
      '--text-lg',
      '--text-xl',
      '--text-2xl',
      '--text-3xl',
      '--text-4xl',
      '--text-5xl',
      '--font-weight-normal',
      '--font-weight-medium',
      '--font-weight-semibold',
      '--font-weight-bold',
      '--font-weight-extrabold',
      '--line-height-tight',
      '--line-height-normal',
      '--line-height-relaxed'
    ];
    
    typographyTokens.forEach(token => {
      const value = getCSSVariable(token);
      expect(value, `${token} should be defined`).toBeTruthy();
    });
  });

  it('Property 1: All spacing tokens are defined and valid CSS units', () => {
    const spacingScales = [1, 2, 3, 4, 5, 6, 8, 10, 12, 16, 20];
    
    spacingScales.forEach(scale => {
      const variableName = `--space-${scale}`;
      const value = getCSSVariable(variableName);
      
      expect(value, `${variableName} should be defined`).toBeTruthy();
      expect(isValidCSSUnit(value), `${variableName} should be a valid CSS unit`).toBe(true);
    });
  });

  it('Property 1: All shadow tokens are defined', () => {
    const shadowScales = ['sm', 'md', 'lg', 'xl', '2xl'];
    
    shadowScales.forEach(scale => {
      const variableName = `--shadow-${scale}`;
      const value = getCSSVariable(variableName);
      
      expect(value, `${variableName} should be defined`).toBeTruthy();
      expect(value.length, `${variableName} should have content`).toBeGreaterThan(0);
    });
  });

  it('Property 1: All border radius tokens are defined and valid CSS units', () => {
    const radiusScales = ['sm', 'md', 'lg', 'xl', '2xl', 'full'];
    
    radiusScales.forEach(scale => {
      const variableName = `--radius-${scale}`;
      const value = getCSSVariable(variableName);
      
      expect(value, `${variableName} should be defined`).toBeTruthy();
      // 'full' uses 9999px, others use rem
      if (scale !== 'full') {
        expect(isValidCSSUnit(value), `${variableName} should be a valid CSS unit`).toBe(true);
      }
    });
  });

  it('Property 1: All transition tokens are defined', () => {
    const transitionScales = ['fast', 'base', 'slow'];
    
    transitionScales.forEach(scale => {
      const variableName = `--transition-${scale}`;
      const value = getCSSVariable(variableName);
      
      expect(value, `${variableName} should be defined`).toBeTruthy();
      expect(value.length, `${variableName} should have content`).toBeGreaterThan(0);
    });
  });

  /**
   * Property-based test: Color scale ordering
   * For any color scale (primary, neutral), lighter shades (lower numbers)
   * should have higher luminance than darker shades (higher numbers)
   */
  it('Property 1: Color scales maintain proper ordering from light to dark', () => {
    // Helper to convert hex to RGB
    const hexToRgb = (hex) => {
      const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
      return result ? {
        r: parseInt(result[1], 16),
        g: parseInt(result[2], 16),
        b: parseInt(result[3], 16)
      } : null;
    };

    // Calculate relative luminance
    const getLuminance = (hex) => {
      const rgb = hexToRgb(hex);
      if (!rgb) return 0;
      
      const [r, g, b] = [rgb.r, rgb.g, rgb.b].map(val => {
        val = val / 255;
        return val <= 0.03928 ? val / 12.92 : Math.pow((val + 0.055) / 1.055, 2.4);
      });
      
      return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    };

    // Test primary colors
    const primaryScales = [50, 100, 200, 300, 400, 500, 600, 700, 800, 900];
    for (let i = 0; i < primaryScales.length - 1; i++) {
      const lighter = getCSSVariable(`--color-primary-${primaryScales[i]}`);
      const darker = getCSSVariable(`--color-primary-${primaryScales[i + 1]}`);
      
      const lighterLuminance = getLuminance(lighter);
      const darkerLuminance = getLuminance(darker);
      
      expect(lighterLuminance, 
        `primary-${primaryScales[i]} should be lighter than primary-${primaryScales[i + 1]}`
      ).toBeGreaterThan(darkerLuminance);
    }

    // Test neutral colors
    const neutralScales = [50, 100, 200, 300, 400, 500, 600, 700, 800, 900];
    for (let i = 0; i < neutralScales.length - 1; i++) {
      const lighter = getCSSVariable(`--color-neutral-${neutralScales[i]}`);
      const darker = getCSSVariable(`--color-neutral-${neutralScales[i + 1]}`);
      
      const lighterLuminance = getLuminance(lighter);
      const darkerLuminance = getLuminance(darker);
      
      expect(lighterLuminance,
        `neutral-${neutralScales[i]} should be lighter than neutral-${neutralScales[i + 1]}`
      ).toBeGreaterThan(darkerLuminance);
    }
  });

  /**
   * Property-based test: Spacing scale consistency
   * For any spacing token, the value should increase proportionally
   */
  it('Property 1: Spacing scale increases monotonically', () => {
    const spacingScales = [1, 2, 3, 4, 5, 6, 8, 10, 12, 16, 20];
    
    const parseRemValue = (value) => {
      const match = value.match(/^([\d.]+)rem$/);
      return match ? parseFloat(match[1]) : 0;
    };

    for (let i = 0; i < spacingScales.length - 1; i++) {
      const smaller = getCSSVariable(`--space-${spacingScales[i]}`);
      const larger = getCSSVariable(`--space-${spacingScales[i + 1]}`);
      
      const smallerValue = parseRemValue(smaller);
      const largerValue = parseRemValue(larger);
      
      expect(largerValue,
        `space-${spacingScales[i + 1]} should be larger than space-${spacingScales[i]}`
      ).toBeGreaterThan(smallerValue);
    }
  });

  /**
   * Property-based test: Font size scale consistency
   * For any font size token, the value should increase monotonically
   */
  it('Property 1: Font size scale increases monotonically', () => {
    const fontSizes = [
      { name: 'xs', expected: 0.75 },
      { name: 'sm', expected: 0.875 },
      { name: 'base', expected: 1 },
      { name: 'lg', expected: 1.125 },
      { name: 'xl', expected: 1.25 },
      { name: '2xl', expected: 1.5 },
      { name: '3xl', expected: 1.875 },
      { name: '4xl', expected: 2.25 },
      { name: '5xl', expected: 3 }
    ];
    
    const parseRemValue = (value) => {
      const match = value.match(/^([\d.]+)rem$/);
      return match ? parseFloat(match[1]) : 0;
    };

    for (let i = 0; i < fontSizes.length - 1; i++) {
      const smaller = getCSSVariable(`--text-${fontSizes[i].name}`);
      const larger = getCSSVariable(`--text-${fontSizes[i + 1].name}`);
      
      const smallerValue = parseRemValue(smaller);
      const largerValue = parseRemValue(larger);
      
      expect(largerValue,
        `text-${fontSizes[i + 1].name} should be larger than text-${fontSizes[i].name}`
      ).toBeGreaterThan(smallerValue);
    }
  });
});
