/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{js,ts,jsx,tsx}",
    "./components/**/*.{js,ts,jsx,tsx}",
    "./pages/**/*.{js,ts,jsx,tsx}",
    "./app/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: "#C98C7B",
          hover: "#B67868"
        },
        bg: "#F6F1EC",
        surface: "#F2EAE4",
        card: "#FFFFFF",
        border: "#E8DDD6",
        textPrimary: "#5A463F",
        textSecondary: "#9C857C",
        error: "#E57373",
        success: "#81C784",
        warning: "#FFB74D"
      },
      borderRadius: {
        sm: "8px",
        md: "16px",
        lg: "24px",
        xl: "32px",
        "2xl": "32px"
      },
      boxShadow: {
        soft: "0 8px 24px rgba(90, 70, 63, 0.04)",
        hover: "0 12px 32px rgba(90, 70, 63, 0.08)",
        floating: "0 24px 48px rgba(90, 70, 63, 0.12)",
        "inner-pressed": "inset 0 4px 8px rgba(90, 70, 63, 0.1)"
      },
      fontFamily: {
        sans: ["Quicksand", "Nunito", "Inter", "sans-serif"],
      },
      keyframes: {
        float: {
          '0%, 100%': { transform: 'translateY(0)' },
          '50%': { transform: 'translateY(-10px)' },
        },
        'scale-bounce': {
          '0%, 100%': { transform: 'scale(1)' },
          '50%': { transform: 'scale(1.1)' },
        }
      },
      animation: {
        float: 'float 3s ease-in-out infinite',
        'scale-bounce': 'scale-bounce 0.3s ease-in-out',
      }
    }
  },
  plugins: [],
};
