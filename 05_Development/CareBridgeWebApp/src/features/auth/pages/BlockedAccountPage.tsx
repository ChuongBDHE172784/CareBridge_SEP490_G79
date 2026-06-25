const BlockedAccountPage = () => {
  const params = new URLSearchParams(window.location.search);
  const reason = params.get('reason');

  const isDisabled = reason === 'disabled';

  const title = isDisabled ? 'Tài khoản bị vô hiệu hoá' : 'Tài khoản bị khoá tạm thời';
  const message = isDisabled
    ? 'Tài khoản của bạn đã bị vô hiệu hoá. Vui lòng liên hệ bộ phận hỗ trợ để được trợ giúp.'
    : 'Tài khoản của bạn đang bị khoá tạm thời. Vui lòng thử lại sau hoặc liên hệ bộ phận hỗ trợ.';

  return (
    <div
      style={{
        minHeight: '100vh',
        background: '#F6F1EC',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '24px',
        fontFamily: 'inherit',
      }}
    >
      <div
        style={{
          background: '#ffffff',
          borderRadius: '32px',
          boxShadow: '0 12px 32px rgba(90,70,63,0.08)',
          padding: '48px 40px',
          maxWidth: '440px',
          width: '100%',
          textAlign: 'center',
        }}
      >
        <div
          style={{
            width: '64px',
            height: '64px',
            borderRadius: '50%',
            background: 'rgba(201,140,123,0.12)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            margin: '0 auto 24px',
          }}
        >
          <svg
            width="28"
            height="28"
            viewBox="0 0 24 24"
            fill="none"
            stroke="#C98C7B"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
            <path d="M7 11V7a5 5 0 0 1 10 0v4" />
          </svg>
        </div>

        <h1
          style={{
            fontSize: '22px',
            fontWeight: 700,
            color: '#5A463F',
            margin: '0 0 12px',
            lineHeight: 1.3,
          }}
        >
          {title}
        </h1>

        <p
          style={{
            fontSize: '15px',
            color: '#9C857C',
            margin: '0 0 32px',
            lineHeight: 1.6,
          }}
        >
          {message}
        </p>

        <button
          onClick={() => { window.location.href = '/login'; }}
          style={{
            display: 'inline-block',
            background: '#C98C7B',
            color: '#ffffff',
            border: 'none',
            borderRadius: '9999px',
            padding: '12px 32px',
            fontSize: '15px',
            fontWeight: 600,
            cursor: 'pointer',
            boxShadow: '0 4px 16px rgba(201,140,123,0.28)',
            transition: 'opacity 0.15s',
          }}
          onMouseOver={(e) => { (e.currentTarget as HTMLButtonElement).style.opacity = '0.85'; }}
          onMouseOut={(e) => { (e.currentTarget as HTMLButtonElement).style.opacity = '1'; }}
        >
          Quay lại đăng nhập
        </button>
      </div>
    </div>
  );
};

export default BlockedAccountPage;
