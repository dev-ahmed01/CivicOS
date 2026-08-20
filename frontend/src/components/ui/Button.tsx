import type { ButtonHTMLAttributes } from 'react'

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: 'primary' | 'secondary'
}

export function Button({ variant = 'primary', className = '', ...props }: ButtonProps) {
  const classes = `button button-${variant} ${className}`.trim()
  return <button className={classes} {...props} />
}
