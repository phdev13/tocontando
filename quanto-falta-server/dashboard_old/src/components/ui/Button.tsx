import { cn } from '../../lib/utils';
import React from 'react';
import { Loader2 } from 'lucide-react';

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'danger' | 'ghost' | 'outline';
  size?: 'sm' | 'md' | 'lg' | 'icon';
  isLoading?: boolean;
}

export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant = 'primary', size = 'md', isLoading, children, disabled, ...props }, ref) => {
    
    const baseClass = "inline-flex items-center justify-center rounded-lg font-medium transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-primary/50 disabled:opacity-60 disabled:pointer-events-none gap-2 cursor-pointer active:scale-95";
    
    const variantClasses = {
      primary: "bg-primary text-white hover:bg-primary-hover shadow-[0_0_15px_rgba(139,92,246,0.3)] hover:shadow-[0_0_25px_rgba(139,92,246,0.6)]",
      secondary: "glass-panel-elevated text-text-primary hover:bg-white/10 hover:border-white/20",
      danger: "bg-danger/10 text-danger border border-danger/20 hover:bg-danger/20 hover:shadow-[0_0_15px_rgba(239,68,68,0.2)]",
      outline: "bg-transparent text-text-primary border border-white/10 hover:bg-white/5 hover:border-white/20",
      ghost: "bg-transparent text-text-primary hover:bg-white/10",
    };

    const sizeClasses = {
      sm: "h-8 px-3 text-xs",
      md: "h-10 px-4 text-sm",
      lg: "h-12 px-6 text-base",
      icon: "h-10 w-10 p-0",
    };

    return (
      <button
        ref={ref}
        className={cn(baseClass, variantClasses[variant], sizeClasses[size], className)}
        disabled={disabled || isLoading}
        {...props}
      >
        {isLoading && <Loader2 size={16} className="animate-spin" />}
        {!isLoading && children}
      </button>
    );
  }
);
Button.displayName = 'Button';
