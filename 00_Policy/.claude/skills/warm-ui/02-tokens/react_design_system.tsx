import React from 'react';

/**
 * MOMCARE CLAY DESIGN SYSTEM - REACT TYPESCRIPT CONVENTIONS
 * Using Tailwind CSS for styling with strict Type checking
 */

export interface ClayProp {
    children?: React.ReactNode;
    className?: string;
    onClick?: () => void;
}

export const ClayColors = {
    primary: '#C98C7B',
    primaryHover: '#B67868',
    bg: '#F6F1EC',
    surface: '#F2EAE4',
    card: '#FFFFFF',
    textPrimary: '#5A463F',
    textSecondary: '#9C857C',
};

/**
 * Standard Clay Card Component
 */
export const Card: React.FC<ClayProp & { floating?: boolean }> = ({
    children,
    className = '',
    floating = false
}) => {
    return (
        <div className={`
      bg-white 
      rounded-[32px] 
      ${floating ? 'shadow-floating' : 'shadow-soft'} 
      border border-[#E8DDD6]/50 
      p-8 
      ${className}
    `}>
            {children}
        </div>
    );
};

/**
 * Standard Pill Button Component
 */
export const Button: React.FC<ClayProp & { variant?: 'primary' | 'secondary' }> = ({
    children,
    className = '',
    variant = 'primary',
    onClick
}) => {
    const baseStyles = 'rounded-full px-8 py-4 font-black text-sm uppercase tracking-widest transition-all active:scale-95 duration-300';
    const variants: Record<string, string> = {
        primary: 'bg-[#C98C7B] text-white shadow-soft hover:bg-[#B67868] hover:-translate-y-1',
        secondary: 'bg-[#F2EAE4] text-[#5A463F] hover:bg-[#E8DDD6]'
    };

    return (
        <button
            onClick={onClick}
            className={`${baseStyles} ${variants[variant]} ${className}`}
        >
            {children}
        </button>
    );
};

// Re-export Lucide Icons wrapper for consistency
export const IconWrapper: React.FC<{ icon: React.ReactNode; color?: string }> = ({
    icon,
    color = 'bg-primary/10 text-primary'
}) => (
    <div className={`w-14 h-14 ${color} rounded-full flex items-center justify-center shrink-0`}>
        {icon}
    </div>
);
