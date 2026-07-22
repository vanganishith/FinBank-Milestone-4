import { useState } from 'react'

const GATEWAY_URL = 'http://localhost:8080'

async function callApi(path, options = {}) {
  try {
    const res = await fetch(`${GATEWAY_URL}${path}`, options)
    const text = await res.text()
    let data
    try { data = JSON.parse(text) } catch { data = text }
    if (!res.ok) {
      return { __error: true, message: `${res.status} ${res.statusText}\n${typeof data === 'string' ? data : JSON.stringify(data, null, 2)}` }
    }
    return data
  } catch (err) {
    return { __error: true, message: `Request failed: ${err.message}\n\nIs the API Gateway running on ${GATEWAY_URL}?` }
  }
}

function ResultBox({ result }) {
  if (result === null || result === undefined) return null
  const isError = result.__error
  return (
    <div className={`result ${isError ? 'error' : ''}`}>
      {isError ? result.message : JSON.stringify(result, null, 2)}
    </div>
  )
}

function StatusPill({ value }) {
  if (!value) return <span className="pill pending">UNSET</span>
  return <span className={`pill ${value.toLowerCase()}`}>{value}</span>
}

/* ---------------- Login ---------------- */
function Login({ onLogin }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const submit = async () => {
    setError('')
    setLoading(true)
    const res = await callApi('/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    })
    setLoading(false)
    if (res.__error || res.error) {
      setError(res.error || 'Login failed. Check credentials.')
      return
    }
    onLogin({ token: res.token, role: res.role, username: res.username })
  }

  return (
    <div className="login-shell">
      <div className="login-card">
        <div className="login-mark">FinBank — Teller Console</div>
        <div className="login-title">Sign in</div>
        <div className="login-sub">Use your teller or manager credentials.</div>

        <label>Username</label>
        <input value={username} onChange={e => setUsername(e.target.value)}
          onKeyDown={e => e.key === 'Enter' && submit()} placeholder="" />
        <label>Password</label>
        <input type="password" value={password} onChange={e => setPassword(e.target.value)}
          onKeyDown={e => e.key === 'Enter' && submit()} placeholder="" />

        <button className="action teal" style={{ width: '100%' }} onClick={submit} disabled={loading}>
          {loading ? 'Signing in…' : 'Sign in'}
        </button>

        {error && <div className="login-error">{error}</div>}

        
      </div>
    </div>
  )
}

/* ---------------- Customer tab ---------------- */
function CustomerTab({ authHeader }) {
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [phone, setPhone] = useState('')
  const [lookupId, setLookupId] = useState('')
  const [kycId, setKycId] = useState('')
  const [result, setResult] = useState(null)

  const createCustomer = async () => {
    setResult(await callApi('/customer/add', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name, email, phone })
    }))
  }
  const getCustomer = async () => lookupId && setResult(await callApi(`/customer/${lookupId}`))
  const verifyKyc = async () => kycId && setResult(await callApi(`/customer/kyc/verify/${kycId}`, { method: 'PUT' }))
  const rejectKyc = async () => kycId && setResult(await callApi(`/customer/kyc/reject/${kycId}`, { method: 'PUT' }))

  return (
    <>
      <div className="ledger-card">
        <h2>Onboard customer</h2>
        <label>Name</label>
        <input value={name} onChange={e => setName(e.target.value)} placeholder="Enter Your Full Name" />
        <label>Email</label>
        <input value={email} onChange={e => setEmail(e.target.value)} placeholder= "" />
        <label>Phone</label>
        <input value={phone} onChange={e => setPhone(e.target.value)} placeholder="Enter 10-digit Number" />
        <button className="action teal" onClick={createCustomer}>Create customer</button>
      </div>

      <div className="ledger-card">
        <h2>Look up customer</h2>
        <label>Customer ID</label>
        <div className="row">
          <input value={lookupId} onChange={e => setLookupId(e.target.value)} placeholder="1" />
          <button className="action" onClick={getCustomer}>Get</button>
        </div>
      </div>

      <div className="ledger-card">
        <h2>KYC verification</h2>
        <label>Customer ID</label>
        <input value={kycId} onChange={e => setKycId(e.target.value)} placeholder="1" />
        <div className="btn-row">
          <button className="action teal" onClick={verifyKyc}>Mark verified</button>
          <button className="action rust" onClick={rejectKyc}>Reject</button>
        </div>
      </div>

      <ResultBox result={result} />
    </>
  )
}

/* ---------------- Account tab ---------------- */
function AccountTab({ authHeader, role }) {
  const [custId, setCustId] = useState('')
  const [accType, setAccType] = useState('SAVINGS')
  const [balance, setBalance] = useState('')
  const [lookupId, setLookupId] = useState('')
  const [lifecycleId, setLifecycleId] = useState('')
  const [auditId, setAuditId] = useState('')
  const [result, setResult] = useState(null)

  const isManager = role === 'MANAGER'

  const createAccount = async () => {
    setResult(await callApi('/account/add', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ custId: Number(custId), accType, balance: Number(balance) })
    }))
  }
  const getAccount = async () => lookupId && setResult(await callApi(`/account/withCustomer/${lookupId}`))

  const lifecycleAction = async (action, needsManager) => {
    if (!lifecycleId) return
    if (needsManager && !isManager) {
      setResult({ __error: true, message: `403 Forbidden\nThis action requires the MANAGER role. You are signed in as ${role}.` })
      return
    }
    setResult(await callApi(`/account/${action}/${lifecycleId}`, {
      method: 'PUT',
      headers: { 'Authorization': authHeader }
    }))
  }

  const getAudit = async () => auditId && setResult(await callApi(`/account/audit/${auditId}`))

  return (
    <>
      <div className="ledger-card">
        <h2>Open account</h2>
        <label>Customer ID</label>
        <input value={custId} onChange={e => setCustId(e.target.value)} placeholder="1" />
        <label>Account type</label>
        <select value={accType} onChange={e => setAccType(e.target.value)}>
          <option value="SAVINGS">SAVINGS</option>
          <option value="CURRENT">CURRENT</option>
        </select>
        <label>Initial balance</label>
        <input value={balance} onChange={e => setBalance(e.target.value)} placeholder="1000" />
        <button className="action teal" onClick={createAccount}>Open account</button>
      </div>

      <div className="ledger-card">
        <h2>Look up account</h2>
        <div className="hint">Enriched with customer details via Feign.</div>
        <label>Account ID</label>
        <div className="row">
          <input value={lookupId} onChange={e => setLookupId(e.target.value)} placeholder="1" />
          <button className="action" onClick={getAccount}>Get</button>
        </div>
      </div>

      <div className="ledger-card">
        <h2>
          Account lifecycle
          {!isManager && <span className="pill pending" style={{ marginLeft: 8 }}>Freeze/close need MANAGER</span>}
        </h2>
        <label>Account ID</label>
        <input value={lifecycleId} onChange={e => setLifecycleId(e.target.value)} placeholder="1" />
        <div className="btn-row">
          <button className="action amber" onClick={() => lifecycleAction('freeze', true)} disabled={!isManager}>Freeze</button>
          <button className="action rust" onClick={() => lifecycleAction('close', true)} disabled={!isManager}>Close</button>
          <button className="action teal" onClick={() => lifecycleAction('reactivate', false)}>Reactivate</button>
        </div>
      </div>

      <div className="ledger-card">
        <h2>Audit trail</h2>
        <label>Account ID</label>
        <div className="row">
          <input value={auditId} onChange={e => setAuditId(e.target.value)} placeholder="1" />
          <button className="action" onClick={getAudit}>View log</button>
        </div>
      </div>

      <ResultBox result={result} />
    </>
  )
}

/* ---------------- Transaction tab ---------------- */
function TransactionTab() {
  const [accId, setAccId] = useState('')
  const [amount, setAmount] = useState('')
  const [historyId, setHistoryId] = useState('')
  const [result, setResult] = useState(null)

  const doTransaction = async (type) => {
    if (!accId || !amount) return
    setResult(await callApi(`/transaction/${type}/${accId}/${amount}`, { method: 'POST' }))
  }
  const getHistory = async () => historyId && setResult(await callApi(`/transaction/account/${historyId}`))

  return (
    <>
      <div className="ledger-card">
        <h2>Deposit / withdraw</h2>
        <label>Account ID</label>
        <input value={accId} onChange={e => setAccId(e.target.value)} placeholder="1" />
        <label>Amount</label>
        <input value={amount} onChange={e => setAmount(e.target.value)} placeholder="500" className="figure" />
        <div className="btn-row">
          <button className="action teal" onClick={() => doTransaction('deposit')}>Deposit</button>
          <button className="action rust" onClick={() => doTransaction('withdraw')}>Withdraw</button>
        </div>
      </div>

      <div className="ledger-card">
        <h2>Transaction history</h2>
        <label>Account ID</label>
        <div className="row">
          <input value={historyId} onChange={e => setHistoryId(e.target.value)} placeholder="1" />
          <button className="action" onClick={getHistory}>View</button>
        </div>
      </div>

      <ResultBox result={result} />
    </>
  )
}

/* ---------------- Shell ---------------- */
export default function App() {
  const [session, setSession] = useState(null)
  const [tab, setTab] = useState('customer')

  if (!session) return <Login onLogin={setSession} />

  const authHeader = `Bearer ${session.token}`
  const titles = {
    customer: ['Customer & KYC', 'Onboard customers and manage verification status.'],
    account: ['Accounts', 'Open accounts, manage lifecycle, and review the audit trail.'],
    transaction: ['Transactions', 'Process deposits, withdrawals, and view statements.'],
  }

  return (
    <div className="shell">
      <div className="sidebar">
        <div className="brand">
          <div className="brand-mark">Teller Console</div>
          <div className="brand-name">FinBank</div>
        </div>
        <div className={`nav-item ${tab === 'customer' ? 'active' : ''}`} onClick={() => setTab('customer')}>Customer & KYC</div>
        <div className={`nav-item ${tab === 'account' ? 'active' : ''}`} onClick={() => setTab('account')}>Accounts</div>
        <div className={`nav-item ${tab === 'transaction' ? 'active' : ''}`} onClick={() => setTab('transaction')}>Transactions</div>

        <div className="session-box">
          <div className="session-role">{session.role}</div>
          <div className="session-name">{session.username}</div>
          <button className="logout-btn" onClick={() => setSession(null)}>Sign out</button>
        </div>
      </div>

      <div className="main">
        <div className="page-title">{titles[tab][0]}</div>
        <div className="page-sub">{titles[tab][1]}</div>

        {tab === 'customer' && <CustomerTab authHeader={authHeader} />}
        {tab === 'account' && <AccountTab authHeader={authHeader} role={session.role} />}
        {tab === 'transaction' && <TransactionTab authHeader={authHeader} />}
      </div>
    </div>
  )
}
