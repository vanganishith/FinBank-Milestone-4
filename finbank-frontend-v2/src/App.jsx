import { useState, useEffect } from 'react'

const GATEWAY_URL = 'http://localhost:8080'

async function callApi(path, options = {}) {
  try {
    const res = await fetch(`${GATEWAY_URL}${path}`, options)
    const text = await res.text()
    let data
    try { data = JSON.parse(text) } catch { data = text }
    if (!res.ok) {
      const msg = (data && typeof data === 'object' && data.message) ? data.message
        : (typeof data === 'string' && data) ? data
        : `Something went wrong (${res.status}).`
      return { __error: true, message: msg }
    }
    return data
  } catch (err) {
    return { __error: true, message: `Couldn't reach the server. Is everything running?` }
  }
}

/* ---------- Shared UI pieces ---------- */
function Badge({ status }) {
  const s = (status || '').toUpperCase()
  const map = {
    ACTIVE: 'ok', VERIFIED: 'ok', APPROVED: 'ok', AUTO_CLEARED: 'ok', SUCCESS: 'ok', SETTLED: 'ok',
    PENDING: 'warn', PENDING_REVIEW: 'warn', FROZEN: 'warn',
    REJECTED: 'danger', CLOSED: 'danger', FAILED: 'danger', NPA: 'danger', FRAUD_BLOCKED: 'danger',
  }
  const kind = map[s] || 'neutral'
  return <span className={`badge badge-${kind}`}>{s.replace(/_/g, ' ')}</span>
}

function Banner({ error, success }) {
  if (!error && !success) return null
  return <div className={`msg-banner ${error ? 'error' : 'success'}`}>{error || success}</div>
}

function EmptyState({ children }) {
  return <div className="state-box">{children}</div>
}

function Btn({ children, onClick, kind = 'primary', loading, disabled, full, type }) {
  return (
    <button
      className={`btn btn-${kind}${full ? ' btn-full' : ''}`}
      onClick={onClick}
      disabled={disabled || loading}
      type={type || 'button'}
    >
      {loading && <span className="spinner" />}
      {children}
    </button>
  )
}

function Field({ label, children }) {
  return <div className="field"><label>{label}</label>{children}</div>
}

/* ---------- Login ---------- */
function Login({ onLogin }) {
  const [mode, setMode] = useState('staff') // staff | customer | register
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [phone, setPhone] = useState('')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [loading, setLoading] = useState(false)

  const submitStaff = async () => {
    setError(''); setLoading(true)
    const res = await callApi('/auth/login', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    })
    setLoading(false)
    if (res.__error || res.error) { setError(res.error || res.message); return }
    onLogin({ token: res.token, role: res.role, username: res.username })
  }

  const submitCustomer = async () => {
    setError(''); setLoading(true)
    const res = await callApi('/auth/customer-login', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    })
    setLoading(false)
    if (res.__error || res.error) { setError(res.error || res.message); return }
    onLogin({ token: res.token, role: res.role, username: res.username, custId: res.custId, kycStatus: res.kycStatus })
  }

  const submitRegister = async () => {
    setError(''); setSuccess(''); setLoading(true)
    const res = await callApi('/customer/register', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name, email, phone, username, password })
    })
    setLoading(false)
    if (res.__error || res.error) { setError(res.error || res.message); return }
    setSuccess('Account created. Sign in below.')
    setMode('customer')
  }

  const submit = () => {
    if (mode === 'staff') submitStaff()
    else if (mode === 'customer') submitCustomer()
    else submitRegister()
  }

  return (
    <div className="login-page">
      <div className="login-box">
        <div className="mark">FB</div>
        <h1>{mode === 'staff' ? 'Staff sign in' : mode === 'customer' ? 'Sign in' : 'Create your account'}</h1>
        <div className="sub">
          {mode === 'staff' ? 'For tellers, managers, and admins.'
            : mode === 'customer' ? 'Access your accounts and payments.'
            : 'Register for online banking access.'}
        </div>

        {mode === 'register' && (
          <>
            <Field label="Full name"><input value={name} onChange={e => setName(e.target.value)} /></Field>
            <Field label="Email"><input value={email} onChange={e => setEmail(e.target.value)} /></Field>
            <Field label="Phone"><input value={phone} onChange={e => setPhone(e.target.value)} /></Field>
          </>
        )}

        <Field label="Username">
          <input value={username} onChange={e => setUsername(e.target.value)} onKeyDown={e => e.key === 'Enter' && submit()} />
        </Field>
        <Field label="Password">
          <input type="password" value={password} onChange={e => setPassword(e.target.value)} onKeyDown={e => e.key === 'Enter' && submit()} />
        </Field>

        <Btn onClick={submit} loading={loading} full>
          {mode === 'register' ? 'Create account' : 'Sign in'}
        </Btn>

        <Banner error={error} success={success} />

        <div className="login-switch">
          {mode !== 'staff' && <div className="row"><span /><a onClick={() => { setMode('staff'); setError(''); setSuccess('') }}>Staff sign in →</a></div>}
          {mode !== 'customer' && <div className="row"><span /><a onClick={() => { setMode('customer'); setError(''); setSuccess('') }}>Customer sign in →</a></div>}
          {mode !== 'register' && <div className="row"><span>New here?</span><a onClick={() => { setMode('register'); setError(''); setSuccess('') }}>Register</a></div>}
        </div>
      </div>
    </div>
  )
}

/* ---------------- Staff: Customer & KYC ---------------- */
function CustomerTab({ authHeader }) {
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [phone, setPhone] = useState('')
  const [creating, setCreating] = useState(false)
  const [createMsg, setCreateMsg] = useState({ error: '', success: '' })

  const [lookupId, setLookupId] = useState('')
  const [customer, setCustomer] = useState(null)
  const [lookupError, setLookupError] = useState('')
  const [looking, setLooking] = useState(false)

  const [kycId, setKycId] = useState('')
  const [rejectReason, setRejectReason] = useState('')
  const [kycBusy, setKycBusy] = useState(false)
  const [kycMsg, setKycMsg] = useState({ error: '', success: '' })

  const createCustomer = async () => {
    if (!name || !email || !phone) return
    setCreating(true); setCreateMsg({ error: '', success: '' })
    const res = await callApi('/customer/add', {
      method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: authHeader },
      body: JSON.stringify({ name, email, phone })
    })
    setCreating(false)
    if (res.__error) { setCreateMsg({ error: res.message }); return }
    setCreateMsg({ success: `Customer created — ID ${res.custId}` })
    setName(''); setEmail(''); setPhone('')
  }

  const getCustomer = async () => {
    if (!lookupId) return
    setLooking(true); setLookupError(''); setCustomer(null)
    const res = await callApi(`/customer/${lookupId}`, { headers: { Authorization: authHeader } })
    setLooking(false)
    if (res.__error) { setLookupError(res.message); return }
    setCustomer(res)
  }

  const verifyKyc = async () => {
    if (!kycId) return
    setKycBusy(true); setKycMsg({ error: '', success: '' })
    const res = await callApi(`/customer/kyc/verify/${kycId}`, { method: 'PUT', headers: { Authorization: authHeader } })
    setKycBusy(false)
    if (res.__error) { setKycMsg({ error: res.message }); return }
    setKycMsg({ success: `Customer ${kycId} KYC marked verified.` })
  }

  const rejectKyc = async () => {
    if (!kycId) return
    setKycBusy(true); setKycMsg({ error: '', success: '' })
    const res = await callApi(`/customer/kyc/reject/${kycId}?reason=${encodeURIComponent(rejectReason)}`, {
      method: 'PUT', headers: { Authorization: authHeader }
    })
    setKycBusy(false)
    if (res.__error) { setKycMsg({ error: res.message }); return }
    setKycMsg({ success: `Customer ${kycId} KYC rejected.` })
  }

  return (
    <>
      <div className="card">
        <h3>Onboard a customer</h3>
        <p className="card-hint">Creates a customer profile without login access. They can register separately or claim this record later.</p>
        <div className="field-row">
          <Field label="Full name"><input value={name} onChange={e => setName(e.target.value)} placeholder="Jane Doe" /></Field>
        </div>
        <div className="field-row">
          <Field label="Email"><input value={email} onChange={e => setEmail(e.target.value)} placeholder="jane@mail.com" /></Field>
          <Field label="Phone"><input value={phone} onChange={e => setPhone(e.target.value)} placeholder="10-digit number" /></Field>
        </div>
        <Btn onClick={createCustomer} loading={creating}>Create customer</Btn>
        <Banner error={createMsg.error} success={createMsg.success} />
      </div>

      <div className="card">
        <h3>Look up a customer</h3>
        <div className="field-row">
          <Field label="Customer ID"><input value={lookupId} onChange={e => setLookupId(e.target.value)} placeholder="1" /></Field>
        </div>
        <Btn onClick={getCustomer} loading={looking} kind="secondary">Search</Btn>
        {lookupError && <Banner error={lookupError} />}
        {customer && (
          <div style={{ marginTop: 16 }}>
            <div className="data-row"><span className="label">Name</span><span className="value" style={{ fontFamily: 'var(--font)' }}>{customer.name}</span></div>
            <div className="data-row"><span className="label">Email</span><span className="value" style={{ fontFamily: 'var(--font)' }}>{customer.email}</span></div>
            <div className="data-row"><span className="label">Phone</span><span className="value" style={{ fontFamily: 'var(--font)' }}>{customer.phone}</span></div>
            <div className="data-row"><span className="label">KYC status</span><Badge status={customer.kycStatus} /></div>
          </div>
        )}
      </div>

      <div className="card">
        <h3>KYC decision</h3>
        <p className="card-hint">Manual override — use the KYC Review tab for the standard document-verification flow.</p>
        <div className="field-row">
          <Field label="Customer ID"><input value={kycId} onChange={e => setKycId(e.target.value)} placeholder="1" /></Field>
        </div>
        <Field label="Rejection reason (only needed to reject)">
          <input value={rejectReason} onChange={e => setRejectReason(e.target.value)} placeholder="Document unclear" />
        </Field>
        <div className="btn-row">
          <Btn onClick={verifyKyc} loading={kycBusy}>Mark verified</Btn>
          <Btn onClick={rejectKyc} loading={kycBusy} kind="danger">Reject</Btn>
        </div>
        <Banner error={kycMsg.error} success={kycMsg.success} />
      </div>
    </>
  )
}

/* ---------------- Staff: Accounts ---------------- */
function AccountTab({ authHeader, role }) {
  const isManager = role === 'MANAGER'
  const [custId, setCustId] = useState('')
  const [accType, setAccType] = useState('SAVINGS')
  const [balance, setBalance] = useState('')
  const [openBusy, setOpenBusy] = useState(false)
  const [openMsg, setOpenMsg] = useState({})

  const [lookupId, setLookupId] = useState('')
  const [account, setAccount] = useState(null)
  const [lookErr, setLookErr] = useState('')
  const [looking, setLooking] = useState(false)

  const [lifecycleId, setLifecycleId] = useState('')
  const [lcBusy, setLcBusy] = useState(false)
  const [lcMsg, setLcMsg] = useState({})

  const [auditId, setAuditId] = useState('')
  const [auditLog, setAuditLog] = useState(null)
  const [auditErr, setAuditErr] = useState('')
  const [auditBusy, setAuditBusy] = useState(false)

  const openAccount = async () => {
    if (!custId || !balance) return
    setOpenBusy(true); setOpenMsg({})
    const res = await callApi('/account/add', {
      method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: authHeader },
      body: JSON.stringify({ custId: Number(custId), accType, balance: Number(balance) })
    })
    setOpenBusy(false)
    if (res.__error) { setOpenMsg({ error: res.message }); return }
    setOpenMsg({ success: `Account opened — ID ${res.accId}` })
  }

  const getAccount = async () => {
    if (!lookupId) return
    setLooking(true); setLookErr(''); setAccount(null)
    const res = await callApi(`/account/withCustomer/${lookupId}`, { headers: { Authorization: authHeader } })
    setLooking(false)
    if (res.__error) { setLookErr(res.message); return }
    setAccount(res)
  }

  const lifecycleAction = async (action, needsManager) => {
    if (!lifecycleId) return
    if (needsManager && !isManager) { setLcMsg({ error: 'This action requires the MANAGER role.' }); return }
    setLcBusy(true); setLcMsg({})
    const res = await callApi(`/account/${action}/${lifecycleId}`, { method: 'PUT', headers: { Authorization: authHeader } })
    setLcBusy(false)
    if (res.__error) { setLcMsg({ error: res.message }); return }
    setLcMsg({ success: `Account ${lifecycleId} ${action}d.` })
  }

  const getAudit = async () => {
    if (!auditId) return
    setAuditBusy(true); setAuditErr(''); setAuditLog(null)
    const res = await callApi(`/account/audit/${auditId}`, { headers: { Authorization: authHeader } })
    setAuditBusy(false)
    if (res.__error) { setAuditErr(res.message); return }
    setAuditLog(res)
  }

  return (
    <>
      <div className="card">
        <h3>Open an account</h3>
        <div className="field-row">
          <Field label="Customer ID"><input value={custId} onChange={e => setCustId(e.target.value)} placeholder="1" /></Field>
          <Field label="Account type">
            <select value={accType} onChange={e => setAccType(e.target.value)}>
              <option value="SAVINGS">Savings</option>
              <option value="CURRENT">Current</option>
            </select>
          </Field>
        </div>
        <Field label="Initial balance"><input value={balance} onChange={e => setBalance(e.target.value)} placeholder="1000" /></Field>
        <Btn onClick={openAccount} loading={openBusy}>Open account</Btn>
        <Banner error={openMsg.error} success={openMsg.success} />
      </div>

      <div className="card">
        <h3>Look up account</h3>
        <p className="card-hint">Includes linked customer details.</p>
        <div className="field-row">
          <Field label="Account ID"><input value={lookupId} onChange={e => setLookupId(e.target.value)} placeholder="1" /></Field>
        </div>
        <Btn onClick={getAccount} loading={looking} kind="secondary">Search</Btn>
        {lookErr && <Banner error={lookErr} />}
        {account && (
          <div style={{ marginTop: 16 }}>
            <div className="data-row"><span className="label">Type</span><span>{account.accType}</span></div>
            <div className="data-row"><span className="label">Balance</span><span className="value">₹{account.balance?.toLocaleString()}</span></div>
            <div className="data-row"><span className="label">Status</span><Badge status={account.status} /></div>
            {account.customer && <div className="data-row"><span className="label">Customer</span><span>{account.customer.name}</span></div>}
          </div>
        )}
      </div>

      <div className="card">
        <h3>Account lifecycle {!isManager && <span className="badge badge-warn">Freeze/close need manager</span>}</h3>
        <Field label="Account ID"><input value={lifecycleId} onChange={e => setLifecycleId(e.target.value)} placeholder="1" /></Field>
        <div className="btn-row">
          <Btn onClick={() => lifecycleAction('freeze', true)} loading={lcBusy} kind="warn" disabled={!isManager}>Freeze</Btn>
          <Btn onClick={() => lifecycleAction('close', true)} loading={lcBusy} kind="danger" disabled={!isManager}>Close</Btn>
          <Btn onClick={() => lifecycleAction('reactivate', false)} loading={lcBusy}>Reactivate</Btn>
        </div>
        <Banner error={lcMsg.error} success={lcMsg.success} />
      </div>

      <div className="card">
        <h3>Audit trail</h3>
        <Field label="Account ID"><input value={auditId} onChange={e => setAuditId(e.target.value)} placeholder="1" /></Field>
        <Btn onClick={getAudit} loading={auditBusy} kind="secondary">View log</Btn>
        {auditErr && <Banner error={auditErr} />}
        {auditLog && (auditLog.length === 0 ? <EmptyState>No audit entries yet.</EmptyState> : (
          <div style={{ marginTop: 12 }}>
            {auditLog.map((a, i) => (
              <div className="data-row" key={i}><span className="label">{a.action}</span><span style={{ fontFamily: 'var(--font)', fontSize: 12 }}>{a.details}</span></div>
            ))}
          </div>
        ))}
      </div>
    </>
  )
}

/* ---------------- Staff: Account applications review ---------------- */
function ReviewApplicationsTab({ authHeader }) {
  const [pending, setPending] = useState([])
  const [error, setError] = useState('')
  const [busyId, setBusyId] = useState(null)

  const load = async () => {
    const res = await callApi('/account/application/pending-review', { headers: { Authorization: authHeader } })
    if (res.__error) { setError(res.message); return }
    setPending(res); setError('')
  }
  useEffect(() => { load() }, [])

  const approve = async (id) => {
    setBusyId(id)
    const res = await callApi(`/account/application/${id}/approve`, { method: 'PUT', headers: { Authorization: authHeader } })
    setBusyId(null)
    if (res.__error) { setError(res.message); return }
    setError(''); load()
  }
  const reject = async (id) => {
    const reason = prompt('Rejection reason:') || 'Not specified'
    setBusyId(id)
    const res = await callApi(`/account/application/${id}/reject?reason=${encodeURIComponent(reason)}`, { method: 'PUT', headers: { Authorization: authHeader } })
    setBusyId(null)
    if (res.__error) { setError(res.message); return }
    setError(''); load()
  }

  return (
    <>
      {error && <Banner error={error} />}
      {pending.length === 0 && !error && <div className="card"><EmptyState>No pending account applications.</EmptyState></div>}
      {pending.map(a => (
        <div className="card accent-pending" key={a.applicationId}>
          <h3>{a.requestedAccType} — Customer {a.custId}</h3>
          <p className="card-hint">Initial deposit ₹{a.initialDeposit} · Documents: {a.documentsSubmitted || 'none noted'}</p>
          <div className="btn-row">
            <Btn onClick={() => approve(a.applicationId)} loading={busyId === a.applicationId}>Approve</Btn>
            <Btn onClick={() => reject(a.applicationId)} loading={busyId === a.applicationId} kind="danger">Reject</Btn>
          </div>
        </div>
      ))}
    </>
  )
}

/* ---------------- Staff: Transactions ---------------- */
function TransactionTab({ authHeader }) {
  const [accId, setAccId] = useState('')
  const [amount, setAmount] = useState('')
  const [txnBusy, setTxnBusy] = useState(false)
  const [txnMsg, setTxnMsg] = useState({})

  const [historyId, setHistoryId] = useState('')
  const [history, setHistory] = useState(null)
  const [histErr, setHistErr] = useState('')
  const [histBusy, setHistBusy] = useState(false)

  const [stmtAccId, setStmtAccId] = useState('')
  const [stmtFrom, setStmtFrom] = useState('')
  const [stmtTo, setStmtTo] = useState('')
  const [statement, setStatement] = useState(null)
  const [stmtErr, setStmtErr] = useState('')
  const [stmtBusy, setStmtBusy] = useState(false)

  const doTxn = async (type) => {
    if (!accId || !amount) return
    setTxnBusy(true); setTxnMsg({})
    const res = await callApi(`/transaction/${type}/${accId}/${amount}`, { method: 'POST', headers: { Authorization: authHeader } })
    setTxnBusy(false)
    if (res.__error) { setTxnMsg({ error: res.message }); return }
    setTxnMsg({ success: `${type === 'deposit' ? 'Deposited' : 'Withdrew'} ₹${amount} — new transaction #${res.txnId}` })
  }

  const getHistory = async () => {
    if (!historyId) return
    setHistBusy(true); setHistErr(''); setHistory(null)
    const res = await callApi(`/transaction/account/${historyId}`, { headers: { Authorization: authHeader } })
    setHistBusy(false)
    if (res.__error) { setHistErr(res.message); return }
    setHistory(res)
  }

  const getStatement = async () => {
    if (!stmtAccId) return
    setStmtBusy(true); setStmtErr(''); setStatement(null)
    const params = new URLSearchParams()
    if (stmtFrom) params.set('from', `${stmtFrom}T00:00:00`)
    if (stmtTo) params.set('to', `${stmtTo}T23:59:59`)
    const qs = params.toString()
    const res = await callApi(`/transaction/statement/${stmtAccId}${qs ? `?${qs}` : ''}`, { headers: { Authorization: authHeader } })
    setStmtBusy(false)
    if (res.__error) { setStmtErr(res.message); return }
    setStatement(res)
  }

  return (
    <>
      <div className="card">
        <h3>Deposit / withdraw</h3>
        <div className="field-row">
          <Field label="Account ID"><input value={accId} onChange={e => setAccId(e.target.value)} placeholder="1" /></Field>
          <Field label="Amount"><input value={amount} onChange={e => setAmount(e.target.value)} placeholder="500" /></Field>
        </div>
        <div className="btn-row">
          <Btn onClick={() => doTxn('deposit')} loading={txnBusy}>Deposit</Btn>
          <Btn onClick={() => doTxn('withdraw')} loading={txnBusy} kind="danger">Withdraw</Btn>
        </div>
        <Banner error={txnMsg.error} success={txnMsg.success} />
      </div>

      <div className="card">
        <h3>Transaction history</h3>
        <div className="field-row"><Field label="Account ID"><input value={historyId} onChange={e => setHistoryId(e.target.value)} placeholder="1" /></Field></div>
        <Btn onClick={getHistory} loading={histBusy} kind="secondary">View history</Btn>
        {histErr && <Banner error={histErr} />}
        {history && (history.length === 0 ? <EmptyState>No transactions yet.</EmptyState> : (
          <div style={{ marginTop: 12 }}>
            {history.map(t => (
              <div className="data-row" key={t.txnId}>
                <span className="label">{t.type} · {new Date(t.txnDate).toLocaleString()}</span>
                <span className="value">₹{t.amount.toLocaleString()}</span>
              </div>
            ))}
          </div>
        ))}
      </div>

      <div className="card">
        <h3>Statement</h3>
        <p className="card-hint">Opening/closing balance and running ledger for a date range.</p>
        <Field label="Account ID"><input value={stmtAccId} onChange={e => setStmtAccId(e.target.value)} placeholder="1" /></Field>
        <div className="field-row">
          <Field label="From (optional)"><input type="date" value={stmtFrom} onChange={e => setStmtFrom(e.target.value)} /></Field>
          <Field label="To (optional)"><input type="date" value={stmtTo} onChange={e => setStmtTo(e.target.value)} /></Field>
        </div>
        <Btn onClick={getStatement} loading={stmtBusy}>Generate statement</Btn>
        {stmtErr && <Banner error={stmtErr} />}
        {statement && (
          <div style={{ marginTop: 16 }}>
            <div className="data-row"><span className="label">Opening balance</span><span className="value">₹{statement.openingBalance?.toLocaleString()}</span></div>
            <div className="data-row"><span className="label">Closing balance</span><span className="value">₹{statement.closingBalance?.toLocaleString()}</span></div>
            {statement.lines.map(l => (
              <div className="data-row" key={l.txnId}>
                <span className="label">{l.type} · {new Date(l.txnDate).toLocaleDateString()}</span>
                <span className="value">₹{l.runningBalance.toLocaleString()}</span>
              </div>
            ))}
          </div>
        )}
      </div>
    </>
  )
}

/* ---------------- Staff: Loans ---------------- */
function LoanTab({ authHeader, role }) {
  const isManager = role === 'MANAGER'
  const [pending, setPending] = useState([])
  const [pendErr, setPendErr] = useState('')
  const [reviewLoanId, setReviewLoanId] = useState('')
  const [creditScore, setCreditScore] = useState('')
  const [reason, setReason] = useState('')
  const [reviewBusy, setReviewBusy] = useState(false)
  const [reviewMsg, setReviewMsg] = useState({})

  const [profileCustId, setProfileCustId] = useState('')
  const [profileAccId, setProfileAccId] = useState('')
  const [profile, setProfile] = useState(null)
  const [profErr, setProfErr] = useState('')
  const [profBusy, setProfBusy] = useState(false)

  const [scheduleLoanId, setScheduleLoanId] = useState('')
  const [schedule, setSchedule] = useState(null)
  const [schedErr, setSchedErr] = useState('')
  const [schedBusy, setSchedBusy] = useState(false)
  const [repaymentId, setRepaymentId] = useState('')
  const [payBusy, setPayBusy] = useState(false)
  const [payMsg, setPayMsg] = useState({})

  const [collectionsWindow, setCollectionsWindow] = useState('30')
  const [collections, setCollections] = useState(null)
  const [collBusy, setCollBusy] = useState(false)

  const loadPending = async () => {
    const res = await callApi('/loan/pending-review', { headers: { Authorization: authHeader } })
    if (res.__error) { setPendErr(res.message); return }
    setPending(res); setPendErr('')
  }
  useEffect(() => { if (isManager) loadPending() }, [])

  const approve = async () => {
    if (!reviewLoanId || !creditScore) return
    setReviewBusy(true); setReviewMsg({})
    const res = await callApi(`/loan/${reviewLoanId}/approve?creditScore=${creditScore}`, { method: 'PUT', headers: { Authorization: authHeader } })
    setReviewBusy(false)
    if (res.__error) { setReviewMsg({ error: res.message }); return }
    setReviewMsg({ success: `Loan ${reviewLoanId} approved and disbursed.` })
    loadPending()
  }
  const reject = async () => {
    if (!reviewLoanId) return
    setReviewBusy(true); setReviewMsg({})
    const res = await callApi(`/loan/${reviewLoanId}/reject?reason=${encodeURIComponent(reason)}`, { method: 'PUT', headers: { Authorization: authHeader } })
    setReviewBusy(false)
    if (res.__error) { setReviewMsg({ error: res.message }); return }
    setReviewMsg({ success: `Loan ${reviewLoanId} rejected.` })
    loadPending()
  }

  const checkProfile = async () => {
    if (!profileCustId || !profileAccId) return
    setProfBusy(true); setProfErr(''); setProfile(null)
    const res = await callApi(`/loan/credit-profile?custId=${profileCustId}&accId=${profileAccId}`, { headers: { Authorization: authHeader } })
    setProfBusy(false)
    if (res.__error) { setProfErr(res.message); return }
    setProfile(res)
  }

  const getSchedule = async () => {
    if (!scheduleLoanId) return
    setSchedBusy(true); setSchedErr(''); setSchedule(null)
    const res = await callApi(`/loan/${scheduleLoanId}/schedule`, { headers: { Authorization: authHeader } })
    setSchedBusy(false)
    if (res.__error) { setSchedErr(res.message); return }
    setSchedule(res)
  }
  const payInstallment = async () => {
    if (!repaymentId) return
    setPayBusy(true); setPayMsg({})
    const res = await callApi(`/loan/repayment/${repaymentId}/pay`, { method: 'PUT', headers: { Authorization: authHeader } })
    setPayBusy(false)
    if (res.__error) { setPayMsg({ error: res.message }); return }
    setPayMsg({ success: `Installment ${repaymentId} marked paid.` })
  }

  const getCollections = async () => {
    setCollBusy(true)
    const res = await callApi(`/loan/collections?upcomingWindowDays=${collectionsWindow || 7}`, { headers: { Authorization: authHeader } })
    setCollBusy(false)
    if (!res.__error) setCollections(res)
  }

  return (
    <>
      {isManager && (
        <div className="card">
          <h3>Pending loan applications</h3>
          {pendErr && <Banner error={pendErr} />}
          {pending.length === 0 && !pendErr && <EmptyState>No applications pending review.</EmptyState>}
          {pending.map(l => (
            <div className="data-row" key={l.loanId}>
              <span className="label">Loan #{l.loanId} · Customer {l.custId}</span>
              <span>₹{l.principal.toLocaleString()} · {l.tenureMonths}mo</span>
            </div>
          ))}
          <div className="field-row" style={{ marginTop: 16 }}>
            <Field label="Loan ID"><input value={reviewLoanId} onChange={e => setReviewLoanId(e.target.value)} placeholder="1" /></Field>
            <Field label="Credit score"><input value={creditScore} onChange={e => setCreditScore(e.target.value)} placeholder="720" /></Field>
          </div>
          <Field label="Rejection reason (only for reject)"><input value={reason} onChange={e => setReason(e.target.value)} /></Field>
          <div className="btn-row">
            <Btn onClick={approve} loading={reviewBusy}>Approve & disburse</Btn>
            <Btn onClick={reject} loading={reviewBusy} kind="danger">Reject</Btn>
          </div>
          <Banner error={reviewMsg.error} success={reviewMsg.success} />
        </div>
      )}

      <div className="card">
        <h3>Credit profile</h3>
        <div className="field-row">
          <Field label="Customer ID"><input value={profileCustId} onChange={e => setProfileCustId(e.target.value)} placeholder="1" /></Field>
          <Field label="Account ID"><input value={profileAccId} onChange={e => setProfileAccId(e.target.value)} placeholder="1" /></Field>
        </div>
        <Btn onClick={checkProfile} loading={profBusy} kind="secondary">Check profile</Btn>
        {profErr && <Banner error={profErr} />}
        {profile && (
          <div style={{ marginTop: 16 }}>
            <div className="data-row"><span className="label">Customer</span><span>{profile.customerName}</span></div>
            <div className="data-row"><span className="label">KYC</span><Badge status={profile.kycStatus} /></div>
            <div className="data-row"><span className="label">Active loans</span><span>{profile.activeLoanCount}</span></div>
            <div className="data-row"><span className="label">Suggestion</span><span style={{ fontFamily: 'var(--font)', fontSize: 12.5 }}>{profile.suggestion}</span></div>
          </div>
        )}
      </div>

      <div className="card">
        <h3>Repayment schedule</h3>
        <div className="field-row"><Field label="Loan ID"><input value={scheduleLoanId} onChange={e => setScheduleLoanId(e.target.value)} placeholder="1" /></Field></div>
        <Btn onClick={getSchedule} loading={schedBusy} kind="secondary">View schedule</Btn>
        {schedErr && <Banner error={schedErr} />}
        {schedule && (schedule.length === 0 ? <EmptyState>No schedule found.</EmptyState> : (
          <div style={{ marginTop: 12 }}>
            {schedule.map(r => (
              <div className="data-row" key={r.repaymentId}>
                <span className="label">#{r.installmentNumber} · Due {r.dueDate}</span>
                <span><Badge status={r.status} /> ₹{r.amount}</span>
              </div>
            ))}
          </div>
        ))}
        <div className="field-row" style={{ marginTop: 16 }}>
          <Field label="Repayment ID"><input value={repaymentId} onChange={e => setRepaymentId(e.target.value)} placeholder="1" /></Field>
        </div>
        <Btn onClick={payInstallment} loading={payBusy}>Mark paid</Btn>
        <Banner error={payMsg.error} success={payMsg.success} />
      </div>

      <div className="card">
        <h3>Collections worklist</h3>
        <p className="card-hint">Overdue and upcoming installments, most overdue first.</p>
        <div className="field-row">
          <Field label="Upcoming window (days)"><input value={collectionsWindow} onChange={e => setCollectionsWindow(e.target.value)} placeholder="30" /></Field>
        </div>
        <Btn onClick={getCollections} loading={collBusy} kind="warn">Load worklist</Btn>
        {collections && (collections.length === 0 ? <EmptyState>Nothing due.</EmptyState> : (
          <div style={{ marginTop: 12 }}>
            {collections.map(c => (
              <div className="data-row" key={c.repaymentId}>
                <span className="label">Loan {c.loanId} · Customer {c.custId}</span>
                <span><Badge status={c.bucket} /> ₹{c.amount}</span>
              </div>
            ))}
          </div>
        ))}
      </div>
    </>
  )
}

/* ---------------- Staff: KYC review ---------------- */
function KycReviewTab({ authHeader, role }) {
  const isManager = role === 'MANAGER'
  const [pending, setPending] = useState([])
  const [error, setError] = useState('')
  const [busyId, setBusyId] = useState(null)

  const load = async () => {
    if (!isManager) return
    const res = await callApi('/kyc/pending-review', { headers: { Authorization: authHeader } })
    if (res.__error) { setError(res.message); return }
    setPending(res); setError('')
  }
  useEffect(() => { load() }, [])

  const approve = async (id) => {
    setBusyId(id)
    const res = await callApi(`/kyc/${id}/approve`, { method: 'PUT', headers: { Authorization: authHeader } })
    setBusyId(null)
    if (res.__error) { setError(res.message); return }
    setError(''); load()
  }
  const reject = async (id) => {
    const reason = prompt('Rejection reason:') || 'Not specified'
    setBusyId(id)
    const res = await callApi(`/kyc/${id}/reject?reason=${encodeURIComponent(reason)}`, { method: 'PUT', headers: { Authorization: authHeader } })
    setBusyId(null)
    if (res.__error) { setError(res.message); return }
    setError(''); load()
  }

  if (!isManager) return <div className="card"><EmptyState>Only managers can review high-risk KYC applications.</EmptyState></div>

  return (
    <>
      {error && <Banner error={error} />}
      {pending.length === 0 && !error && <div className="card"><EmptyState>No high-risk applications pending review.</EmptyState></div>}
      {pending.map(a => (
        <div className="card accent-pending" key={a.applicationId}>
          <h3>Customer {a.custId} — {a.documentType} <span className="badge badge-danger">High risk</span></h3>
          <p className="card-hint">OCR: {a.ocrStatus} · Face match {a.faceMatchScore}% · Liveness {a.livenessPassed ? 'passed' : 'failed'} · Risk {a.riskScore}</p>
          <div style={{ display: 'flex', gap: 16, alignItems: 'flex-start', marginTop: 12 }}>
            <div>
              <p className="card-hint" style={{ marginBottom: 4 }}>Document photo</p>
              <img src={a.documentImageBase64} alt="document" style={{ width: 140, borderRadius: 8 }} />
            </div>
            <div>
              <p className="card-hint" style={{ marginBottom: 4 }}>Live selfie</p>
              <img src={a.selfieImageBase64} alt="selfie" style={{ width: 100, borderRadius: 8 }} />
            </div>
            {a.supportingDocumentBase64 && (
              <div>
                <p className="card-hint" style={{ marginBottom: 4 }}>Supporting document</p>
                <a href={a.supportingDocumentBase64} download={`supporting-doc-${a.applicationId}.pdf`}>
                  <Btn kind="secondary">View PDF</Btn>
                </a>
              </div>
            )}
          </div>
          <div className="btn-row">
            <Btn onClick={() => approve(a.applicationId)} loading={busyId === a.applicationId}>Approve anyway</Btn>
            <Btn onClick={() => reject(a.applicationId)} loading={busyId === a.applicationId} kind="danger">Reject</Btn>
          </div>
        </div>
      ))}
    </>
  )
}

/* ---------------- Staff: Banking Command dashboard ---------------- */
function BankingCommandTab({ authHeader, role }) {
  const [stats, setStats] = useState(null)
  const [error, setError] = useState('')

  const load = async () => {
    const isManager = role === 'MANAGER'
    const [acc, loan, pay, kyc] = await Promise.all([
      callApi('/account/stats', { headers: { Authorization: authHeader } }),
      callApi('/loan/stats', { headers: { Authorization: authHeader } }),
      callApi('/payment/stats', { headers: { Authorization: authHeader } }),
      isManager ? callApi('/kyc/stats', { headers: { Authorization: authHeader } }) : Promise.resolve(null)
    ])
    if (acc.__error || loan.__error || pay.__error) setError('Some stats failed to load.')
    else setError('')
    setStats({ acc, loan, pay, kyc })
  }
  useEffect(() => { load() }, [])

  if (!stats) return <div className="card"><EmptyState>Loading…</EmptyState></div>

  return (
    <>
      {error && <Banner error={error} />}
      <div className="card">
        <h3>Account Service</h3>
        <div className="stat-grid">
          <div className="stat-item"><div className="num">{stats.acc.totalAccounts ?? '—'}</div><div className="lbl">Total accounts</div></div>
          <div className="stat-item"><div className="num">{stats.acc.activeAccounts ?? '—'}</div><div className="lbl">Active</div></div>
          <div className="stat-item"><div className="num">₹{(stats.acc.totalBalance ?? 0).toLocaleString()}</div><div className="lbl">Total balance</div></div>
        </div>
      </div>
      <div className="card">
        <h3>Loan Service</h3>
        <div className="stat-grid">
          <div className="stat-item"><div className="num">{stats.loan.totalLoans ?? '—'}</div><div className="lbl">Total loans</div></div>
          <div className="stat-item"><div className="num">{stats.loan.activeLoans ?? '—'}</div><div className="lbl">Active</div></div>
          <div className="stat-item"><div className="num">{stats.loan.npaLoans ?? '—'}</div><div className="lbl">NPA</div></div>
          <div className="stat-item"><div className="num">₹{(stats.loan.totalDisbursed ?? 0).toLocaleString()}</div><div className="lbl">Disbursed</div></div>
        </div>
      </div>
      <div className="card">
        <h3>Payment Service</h3>
        <div className="stat-grid">
          <div className="stat-item"><div className="num">{stats.pay.totalPayments ?? '—'}</div><div className="lbl">Total payments</div></div>
          <div className="stat-item"><div className="num">{stats.pay.successRate ?? '—'}%</div><div className="lbl">Success rate</div></div>
          <div className="stat-item"><div className="num">₹{(stats.pay.totalVolume ?? 0).toLocaleString()}</div><div className="lbl">Volume</div></div>
        </div>
      </div>
      {stats.kyc && (
        <div className="card">
          <h3>KYC Service</h3>
          <div className="stat-grid">
            <div className="stat-item"><div className="num">{stats.kyc.pendingReview ?? '—'}</div><div className="lbl">Pending review</div></div>
          </div>
        </div>
      )}
    </>
  )
}

/* ---------------- Admin: Staff management ---------------- */
function AdminTab({ authHeader }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [role, setRole] = useState('TELLER')
  const [loading, setLoading] = useState(false)
  const [msg, setMsg] = useState({})

  const createStaff = async () => {
    if (!username || !password) return
    setLoading(true); setMsg({})
    const res = await callApi('/auth/register', {
      method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: authHeader },
      body: JSON.stringify({ username, password, role })
    })
    setLoading(false)
    if (res.__error || res.error) { setMsg({ error: res.error || res.message }); return }
    setMsg({ success: `${role} account "${res.username}" created.` })
    setUsername(''); setPassword('')
  }

  return (
    <div className="card">
      <h3>Create staff account</h3>
      <p className="card-hint">New teller, manager, or admin accounts.</p>
      <div className="field-row">
        <Field label="Username"><input value={username} onChange={e => setUsername(e.target.value)} placeholder="teller2" /></Field>
        <Field label="Password"><input type="password" value={password} onChange={e => setPassword(e.target.value)} /></Field>
      </div>
      <Field label="Role">
        <select value={role} onChange={e => setRole(e.target.value)}>
          <option value="TELLER">Teller</option>
          <option value="MANAGER">Manager</option>
          <option value="ADMIN">Admin</option>
        </select>
      </Field>
      <Btn onClick={createStaff} loading={loading}>Create account</Btn>
      <Banner error={msg.error} success={msg.success} />
    </div>
  )
}

/* ---------------- Customer: My Accounts ---------------- */
function MyAccountsTab({ custId, authHeader }) {
  const [accounts, setAccounts] = useState([])
  const [error, setError] = useState('')
  const load = async () => {
    const res = await callApi(`/account/customer/${custId}`, { headers: { Authorization: authHeader } })
    if (res.__error) { setError(res.message); return }
    setAccounts(res); setError('')
  }
  useEffect(() => { load() }, [])

  return (
    <>
      {error && <Banner error={error} />}
      {accounts.length === 0 && !error && <div className="card"><EmptyState>No accounts yet — apply for one from the sidebar.</EmptyState></div>}
      {accounts.map(acc => (
        <div className={`card accent-${acc.status === 'ACTIVE' ? 'active' : acc.status === 'CLOSED' ? 'rejected' : 'pending'}`} key={acc.accId}>
          <h3>{acc.accType} account <Badge status={acc.status} /></h3>
          <p className="card-hint">Account ID {acc.accId}</p>
          <div className="stat-item"><div className="num" style={{ fontSize: 26 }}>₹{acc.balance.toLocaleString()}</div></div>
        </div>
      ))}
    </>
  )
}

/* ---------------- Customer: Apply for account ---------------- */
function ApplyAccountTab({ custId, authHeader, onNext }) {
  const [accType, setAccType] = useState('SAVINGS')
  const [initialDeposit, setInitialDeposit] = useState('')

  const next = () => {
    onNext({ accType, initialDeposit })
  }

  return (
    <div className="card">
      <h3>Apply for a new account</h3>
      <p className="card-hint">Choose your account type. You'll complete KYC verification on the next step.</p>
      <div className="field-row">
        <Field label="Account type">
          <select value={accType} onChange={e => setAccType(e.target.value)}>
            <option value="SAVINGS">Savings</option>
            <option value="CURRENT">Current</option>
          </select>
        </Field>
        <Field label="Initial deposit">
          <input value={initialDeposit} onChange={e => setInitialDeposit(e.target.value)} placeholder="1000" />
        </Field>
      </div>
      <Btn onClick={next}>Next</Btn>
    </div>
  )
}

/* ---------------- Customer: Transactions (view only) ---------------- */
function MyTransactionsTab({ authHeader }) {
  const [accId, setAccId] = useState('')
  const [history, setHistory] = useState(null)
  const [statement, setStatement] = useState(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const getHistory = async () => {
    if (!accId) return
    setBusy(true); setError(''); setStatement(null)
    const res = await callApi(`/transaction/account/${accId}`, { headers: { Authorization: authHeader } })
    setBusy(false)
    if (res.__error) { setError(res.message); return }
    setHistory(res)
  }
  const getStatement = async () => {
    if (!accId) return
    setBusy(true); setError(''); setHistory(null)
    const res = await callApi(`/transaction/statement/${accId}`, { headers: { Authorization: authHeader } })
    setBusy(false)
    if (res.__error) { setError(res.message); return }
    setStatement(res)
  }

  return (
    <div className="card">
      <h3>History & statement</h3>
      <p className="card-hint">Deposits and withdrawals are handled by a teller at your branch.</p>
      <Field label="Account ID"><input value={accId} onChange={e => setAccId(e.target.value)} placeholder="17" /></Field>
      <div className="btn-row">
        <Btn onClick={getHistory} loading={busy} kind="secondary">Transaction history</Btn>
        <Btn onClick={getStatement} loading={busy} kind="secondary">Statement</Btn>
      </div>
      {error && <Banner error={error} />}
      {history && (history.length === 0 ? <EmptyState>No transactions yet.</EmptyState> : (
        <div style={{ marginTop: 12 }}>{history.map(t => (
          <div className="data-row" key={t.txnId}><span className="label">{t.type} · {new Date(t.txnDate).toLocaleDateString()}</span><span className="value">₹{t.amount.toLocaleString()}</span></div>
        ))}</div>
      ))}
      {statement && (
        <div style={{ marginTop: 16 }}>
          <div className="data-row"><span className="label">Opening</span><span className="value">₹{statement.openingBalance?.toLocaleString()}</span></div>
          <div className="data-row"><span className="label">Closing</span><span className="value">₹{statement.closingBalance?.toLocaleString()}</span></div>
        </div>
      )}
    </div>
  )
}

/* ---------------- Customer: Loans ---------------- */
function MyLoansTab({ custId, authHeader }) {
  const [accId, setAccId] = useState('')
  const [principal, setPrincipal] = useState('')
  const [interestRate, setInterestRate] = useState('')
  const [tenureMonths, setTenureMonths] = useState('')
  const [loans, setLoans] = useState([])
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const load = async () => {
    const res = await callApi(`/loan/customer/${custId}`, { headers: { Authorization: authHeader } })
    if (!res.__error) setLoans(res)
  }
  useEffect(() => { load() }, [])

  const apply = async () => {
    if (!accId || !principal || !interestRate || !tenureMonths) return
    setLoading(true); setError('')
    const params = new URLSearchParams({ custId, accId, principal, interestRate, tenureMonths })
    const res = await callApi(`/loan/apply?${params.toString()}`, { method: 'POST', headers: { Authorization: authHeader } })
    setLoading(false)
    if (res.__error) { setError(res.message); return }
    load()
  }

  return (
    <>
      <div className="card">
        <h3>Apply for a loan</h3>
        <p className="card-hint">Goes to a manager for review — check back here for the decision.</p>
        <div className="field-row">
          <Field label="Account ID"><input value={accId} onChange={e => setAccId(e.target.value)} placeholder="17" /></Field>
          <Field label="Principal"><input value={principal} onChange={e => setPrincipal(e.target.value)} placeholder="10000" /></Field>
        </div>
        <div className="field-row">
          <Field label="Interest rate (%)"><input value={interestRate} onChange={e => setInterestRate(e.target.value)} placeholder="8.5" /></Field>
          <Field label="Tenure (months)"><input value={tenureMonths} onChange={e => setTenureMonths(e.target.value)} placeholder="12" /></Field>
        </div>
        <Btn onClick={apply} loading={loading}>Submit application</Btn>
        {error && <Banner error={error} />}
      </div>
      {loans.length === 0 && !error && <div className="card"><EmptyState>No loan applications yet.</EmptyState></div>}
      {loans.map(l => (
        <div className={`card accent-${l.status === 'ACTIVE' ? 'active' : l.status === 'REJECTED' ? 'rejected' : 'pending'}`} key={l.loanId}>
          <h3>Loan #{l.loanId} <Badge status={l.status} /></h3>
          <p className="card-hint">₹{l.principal.toLocaleString()} · {l.interestRate}% · {l.tenureMonths}mo</p>
          {l.emi && <p className="card-hint">EMI ₹{l.emi}</p>}
          {l.rejectionReason && <p className="card-hint" style={{ color: 'var(--danger)' }}>{l.rejectionReason}</p>}
        </div>
      ))}
    </>
  )
}

/* ---------------- Customer: Payments ---------------- */
function MyPaymentsTab({ custId, authHeader }) {
  const [benName, setBenName] = useState('')
  const [benAcc, setBenAcc] = useState('')
  const [benNick, setBenNick] = useState('')
  const [beneficiaries, setBeneficiaries] = useState([])
  const [benBusy, setBenBusy] = useState(false)
  const [benMsg, setBenMsg] = useState({})

  const [fromAccId, setFromAccId] = useState('')
  const [toAccId, setToAccId] = useState('')
  const [amount, setAmount] = useState('')
  const [payBusy, setPayBusy] = useState(false)
  const [payMsg, setPayMsg] = useState({})

  const loadBeneficiaries = async () => {
    const res = await callApi(`/payment/beneficiary/${custId}`, { headers: { Authorization: authHeader } })
    if (!res.__error) setBeneficiaries(res)
  }
  useEffect(() => { loadBeneficiaries() }, [])

  const addBeneficiary = async () => {
    if (!benName || !benAcc) return
    setBenBusy(true); setBenMsg({})
    const res = await callApi('/payment/beneficiary/add', {
      method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: authHeader },
      body: JSON.stringify({ custId, name: benName, accountNumber: Number(benAcc), nickname: benNick })
    })
    setBenBusy(false)
    if (res.__error) { setBenMsg({ error: res.message }); return }
    setBenMsg({ success: 'Beneficiary added.' }); setBenName(''); setBenAcc(''); setBenNick('')
    loadBeneficiaries()
  }

  const transfer = async () => {
    if (!fromAccId || !toAccId || !amount) return
    setPayBusy(true); setPayMsg({})
    const res = await callApi(`/payment/transfer?fromAccId=${fromAccId}&toAccId=${toAccId}&amount=${amount}&method=IMPS`,
      { method: 'POST', headers: { Authorization: authHeader } })
    setPayBusy(false)
    if (res.__error) { setPayMsg({ error: res.message }); return }
    if (res.status === 'FRAUD_BLOCKED') { setPayMsg({ error: 'Transfer blocked by fraud check.' }); return }
    setPayMsg({ success: `₹${amount} sent — UTR ${res.utr || res.paymentId}` })
  }

  return (
    <>
      <div className="card">
        <h3>Send money</h3>
        <div className="field-row">
          <Field label="From account (yours)"><input value={fromAccId} onChange={e => setFromAccId(e.target.value)} placeholder="17" /></Field>
          <Field label="To account"><input value={toAccId} onChange={e => setToAccId(e.target.value)} placeholder="1" /></Field>
        </div>
        <Field label="Amount"><input value={amount} onChange={e => setAmount(e.target.value)} placeholder="500" /></Field>
        <Btn onClick={transfer} loading={payBusy}>Send</Btn>
        <Banner error={payMsg.error} success={payMsg.success} />
      </div>

      <div className="card">
        <h3>Beneficiaries</h3>
        <div className="field-row">
          <Field label="Name"><input value={benName} onChange={e => setBenName(e.target.value)} /></Field>
          <Field label="Account number"><input value={benAcc} onChange={e => setBenAcc(e.target.value)} /></Field>
        </div>
        <Field label="Nickname"><input value={benNick} onChange={e => setBenNick(e.target.value)} /></Field>
        <Btn onClick={addBeneficiary} loading={benBusy} kind="secondary">Add beneficiary</Btn>
        <Banner error={benMsg.error} success={benMsg.success} />
        {beneficiaries.length > 0 && (
          <div style={{ marginTop: 16 }}>
            {beneficiaries.map(b => (
              <div className="data-row" key={b.beneficiaryId}><span className="label">{b.name} ({b.nickname})</span><span>Account {b.accountNumber}</span></div>
            ))}
          </div>
        )}
      </div>
    </>
  )
}

/* ---------------- Customer: Notifications ---------------- */
function NotificationsTab({ custId, authHeader }) {
  const [notifications, setNotifications] = useState([])
  const [error, setError] = useState('')

  const loadAll = async () => {
    const accRes = await callApi(`/account/customer/${custId}`, { headers: { Authorization: authHeader } })
    if (accRes.__error) { setError(accRes.message); return }
    const all = []
    for (const acc of accRes) {
      const notifRes = await callApi(`/account/notifications/${acc.accId}`, { headers: { Authorization: authHeader } })
      if (!notifRes.__error) all.push(...notifRes)
    }
    all.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
    setNotifications(all); setError('')
  }
  useEffect(() => {
    loadAll()
    const interval = setInterval(loadAll, 15000)
    return () => clearInterval(interval)
  }, [])

  return (
    <>
      {error && <Banner error={error} />}
      {notifications.length === 0 && !error && <div className="card"><EmptyState>No notifications yet.</EmptyState></div>}
      {notifications.map(n => (
        <div className={`card accent-${n.type === 'PAYMENT_RECEIVED' ? 'active' : 'pending'}`} key={n.notificationId}>
          <h3>{n.type.replace(/_/g, ' ')}</h3>
          <p className="card-hint">{n.message}</p>
          <p className="card-hint" style={{ margin: 0 }}>{new Date(n.createdAt).toLocaleString()}</p>
        </div>
      ))}
    </>
  )
}

/* ---------------- Customer: KYC ---------------- */
function MyKycTab({ custId, authHeader, pendingAccountApp }) {
  const [documentType, setDocumentType] = useState('AADHAAR')
  const [documentReference, setDocumentReference] = useState('')
  const [documentMimeType, setDocumentMimeType] = useState(null)
  const [selfieImage, setSelfieImage] = useState(null)
  const [motionDetected, setMotionDetected] = useState(false)
  const [capturing, setCapturing] = useState(false)
  const [applications, setApplications] = useState([])
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const videoRef = useState(() => document.createElement('video'))[0]
  const streamRef = useState({ current: null })[0]
  const [documentImage, setDocumentImage] = useState(null)
  const [supportingDoc, setSupportingDoc] = useState(null)

  const latest = applications[0]
  const isLocked = latest && ['PENDING', 'UNDER_REVIEW', 'AUTO_CLEARED', 'APPROVED'].includes(latest.status)

  const load = async () => {
    const res = await callApi(`/kyc/customer/${custId}`, { headers: { Authorization: authHeader } })
    if (!res.__error) setApplications(res)
  }
  useEffect(() => { load() }, [])

  const handleDocUpload = (e) => {
    const file = e.target.files[0]
    if (!file) return
    const reader = new FileReader()
    reader.onload = () => setDocumentImage(reader.result)
    reader.readAsDataURL(file)
  }

  const handleSupportingDocUpload = (e) => {
    const file = e.target.files[0]
    if (!file) return
    const reader = new FileReader()
    reader.onload = () => setSupportingDoc(reader.result)
    reader.readAsDataURL(file)
  }

  const startLivenessCapture = async () => {
    setMotionDetected(false); setSelfieImage(null); setCapturing(true)
    const stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'user' } })
    streamRef.current = stream
    videoRef.srcObject = stream
    await videoRef.play()

    const canvas = document.createElement('canvas')
    canvas.width = 320; canvas.height = 240
    const ctx = canvas.getContext('2d')
    let prevFrame = null
    let motionFrames = 0

    const checkFrame = () => {
      if (!streamRef.current) return
      ctx.drawImage(videoRef, 0, 0, 320, 240)
      const frame = ctx.getImageData(0, 0, 320, 240).data
      if (prevFrame) {
        let diff = 0
        for (let i = 0; i < frame.length; i += 400) diff += Math.abs(frame[i] - prevFrame[i])
        if (diff > 1500) motionFrames++
      }
      prevFrame = frame
      if (motionFrames >= 3) {
        setSelfieImage(canvas.toDataURL('image/jpeg'))
        setMotionDetected(true)
        stream.getTracks().forEach(t => t.stop())
        streamRef.current = null
        setCapturing(false)
        return
      }
      requestAnimationFrame(checkFrame)
    }
    requestAnimationFrame(checkFrame)
  }

  const submit = async () => {
    if (!documentReference || !documentImage || !selfieImage || !motionDetected) return
    if (documentType === 'AADHAAR' && documentReference.replace(/\s/g, '').length !== 12) {
      setError('Aadhaar number must be exactly 12 digits.')
      return
    }
    setLoading(true); setError('')
    const res = await callApi('/kyc/submit', {
      method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: authHeader },
      body: JSON.stringify({
        custId, documentType, documentReference,
        documentUploaded: true, selfieCaptured: true,
        documentImageBase64: documentImage, selfieImageBase64: selfieImage, livenessMotionDetected: motionDetected,
        supportingDocumentBase64: supportingDoc,
        requestedAccType: pendingAccountApp?.accType || null,
        initialDeposit: pendingAccountApp?.initialDeposit || null
      })
    })
    setLoading(false)
    if (res.__error) { setError(res.message); return }
    setDocumentReference(''); setDocumentImage(null); setSelfieImage(null); setMotionDetected(false); setSupportingDoc(null)
    load()
  }

  return (
    <>
      {pendingAccountApp && !isLocked && (
        <div className="msg-banner success" style={{ marginBottom: 16 }}>
          Opening a {pendingAccountApp.accType} account with ₹{pendingAccountApp.initialDeposit} deposit once verified.
        </div>
      )}

      {isLocked ? (
        <div className="card accent-active">
          <h3>{latest.documentType} <Badge status={latest.status} /></h3>
          <p className="card-hint">
            {latest.status === 'AUTO_CLEARED' || latest.status === 'APPROVED'
              ? 'Your KYC is verified and your account has been created automatically.'
              : 'Your submission is under review. Check back soon.'}
          </p>
        </div>
      ) : (
        <div className="card">
          <h3>Submit KYC document</h3>
          <p className="card-hint">Upload your document, then complete a live camera check — blink or move your head naturally.</p>

          <div className="field-row">
            <Field label="Document type">
              <select value={documentType} onChange={e => setDocumentType(e.target.value)}>
                <option value="AADHAAR">Aadhaar</option>
                <option value="PAN">PAN</option>
                <option value="PASSPORT">Passport</option>
              </select>
            </Field>
            <Field label="Reference number">
              <input value={documentReference} onChange={e => setDocumentReference(e.target.value)} placeholder="123456789012" />
            </Field>
            {documentType === 'AADHAAR' && documentReference && documentReference.replace(/\s/g, '').length !== 12 && (
              <p className="card-hint" style={{ color: 'var(--warn)' }}>Aadhaar number must be 12 digits ({documentReference.replace(/\s/g, '').length}/12).</p>
            )}
          </div>

          <Field label="Document photo">
            <input type="file" accept="image/*,application/pdf" onChange={handleDocUpload} />
          </Field>
          <Field label="Document photo (used for face match — must show your face clearly)">
            <input type="file" accept="image/*" onChange={handleDocUpload} />
          </Field>
          {documentImage && <img src={documentImage} alt="document" style={{ maxWidth: 200, borderRadius: 8, marginBottom: 12 }} />}

          <Field label="Supporting document (optional — PDF, e.g. address proof)">
            <input type="file" accept="application/pdf" onChange={handleSupportingDocUpload} />
          </Field>
          {supportingDoc && (
            <div className="msg-banner success" style={{ marginBottom: 12 }}>📄 Supporting PDF attached.</div>
          )}
          {documentImage && documentMimeType === 'application/pdf' && (
            <div className="msg-banner success" style={{ marginBottom: 12 }}>
              📄 PDF document attached — ready to submit.
            </div>
          )}
          {documentImage && documentMimeType !== 'application/pdf' && (
            <img src={documentImage} alt="document" style={{ maxWidth: 200, borderRadius: 8, marginBottom: 12 }} />
          )}

          <Field label="Live selfie check">
            <Btn onClick={startLivenessCapture} loading={capturing} kind="secondary">
              {motionDetected ? 'Retake liveness check' : 'Start camera & verify liveness'}
            </Btn>
          </Field>
          {selfieImage && <img src={selfieImage} alt="selfie" style={{ maxWidth: 160, borderRadius: 8, marginBottom: 8 }} />}
          {motionDetected && <p className="card-hint" style={{ color: 'var(--ok)' }}>Movement detected — liveness confirmed.</p>}

          <Btn onClick={submit} loading={loading} disabled={!documentImage || !motionDetected}>Submit for verification</Btn>
          {error && <Banner error={error} />}
        </div>
      )}

      {applications.map(a => (
        <div className={`card accent-${a.status === 'AUTO_CLEARED' || a.status === 'APPROVED' ? 'active' : a.status === 'REJECTED' ? 'rejected' : 'pending'}`} key={a.applicationId}>
          <h3>{a.documentType} <Badge status={a.status} /></h3>
          <p className="card-hint">Risk: {a.riskLevel}</p>
          {a.rejectionReason && <p className="card-hint" style={{ color: 'var(--danger)' }}>{a.rejectionReason}</p>}
        </div>
      ))}
    </>
  )
}

/* ---------------- Shell ---------------- */
export default function App() {
  const [session, setSession] = useState(null)
  const [tab, setTab] = useState(null)
  const [pendingAccountApp, setPendingAccountApp] = useState(null)

  if (!session) return <Login onLogin={setSession} />

  const authHeader = `Bearer ${session.token}`
  const isCustomer = session.role === 'CUSTOMER'
  const isAdmin = session.role === 'ADMIN'

  const staffNav = [
    { key: 'dashboard', label: 'Banking Command', title: 'Banking Command', sub: 'Live stats across every service.' },
    { key: 'customer', label: 'Customer & KYC', title: 'Customer & KYC', sub: 'Onboard customers and manage verification.' },
    { key: 'account', label: 'Accounts', title: 'Accounts', sub: 'Open accounts, manage lifecycle, review audit trail.' },
    { key: 'applications', label: 'Account applications', title: 'Account applications', sub: 'Review digital account applications.' },
    { key: 'transaction', label: 'Transactions', title: 'Transactions', sub: 'Process deposits, withdrawals, statements.' },
    { key: 'loan', label: 'Loans', title: 'Loans', sub: 'Review applications, repayments, NPA risk.' },
    { key: 'kyc', label: 'KYC review', title: 'KYC review', sub: 'Decide on high-risk KYC applications.' },
  ]
  const custNav = [
    { key: 'myaccounts', label: 'My accounts', title: 'My accounts', sub: 'View your accounts.' },
    { key: 'mykyc', label: 'KYC Verification', title: 'KYC Verification', sub: 'Submit documents and complete a live selfie check.' },
    { key: 'applyaccount', label: 'Open an account', title: 'Open an account', sub: 'Apply for a new account digitally.' },
    { key: 'mytransactions', label: 'Transactions', title: 'Transactions', sub: 'History and statements.' },
    { key: 'myloans', label: 'My loans', title: 'My loans', sub: 'Apply and track loan status.' },
    { key: 'mypayments', label: 'Payments', title: 'Payments', sub: 'Beneficiaries and transfers.' },
    { key: 'notifications', label: 'Notifications', title: 'Notifications', sub: 'Recent account and payment activity.' },
  ]
  const adminNav = [
    { key: 'admin', label: 'Staff management', title: 'Staff management', sub: 'Create teller, manager, and admin accounts.' },
  ]

  const nav = isCustomer ? custNav : isAdmin ? adminNav : staffNav
  const activeKey = tab || nav[0]?.key
  const activeItem = nav.find(n => n.key === activeKey)

  return (
    <div className="app-shell">
      <div className="side-nav">
        <div className="side-nav-brand">
          <div className="mark">FB</div>
          <div className="name">FinBank</div>
          <div className="role-tag">{session.role}</div>
        </div>
        <div className="side-nav-items">
          {nav.map(item => (
            <button key={item.key} className={`nav-btn${activeKey === item.key ? ' active' : ''}`} onClick={() => setTab(item.key)}>
              {item.label}
            </button>
          ))}
        </div>
        <div className="side-nav-footer">
          <div className="user-name">{session.username}</div>
          <button className="btn-signout" onClick={() => setSession(null)}>Sign out</button>
        </div>
      </div>

      <div className="main-content">
        {activeItem && <div className="page-head"><h1>{activeItem.title}</h1><p>{activeItem.sub}</p></div>}

        {!isCustomer && !isAdmin && activeKey === 'dashboard' && <BankingCommandTab authHeader={authHeader} role={session.role} />}
        {!isCustomer && !isAdmin && activeKey === 'customer' && <CustomerTab authHeader={authHeader} />}
        {!isCustomer && !isAdmin && activeKey === 'account' && <AccountTab authHeader={authHeader} role={session.role} />}
        {!isCustomer && !isAdmin && activeKey === 'applications' && <ReviewApplicationsTab authHeader={authHeader} />}
        {!isCustomer && !isAdmin && activeKey === 'transaction' && <TransactionTab authHeader={authHeader} />}
        {!isCustomer && !isAdmin && activeKey === 'loan' && <LoanTab authHeader={authHeader} role={session.role} />}
        {!isCustomer && !isAdmin && activeKey === 'kyc' && <KycReviewTab authHeader={authHeader} role={session.role} />}
          
        {isCustomer && activeKey === 'applyaccount' && (
          <ApplyAccountTab custId={session.custId} authHeader={authHeader}
            onNext={(data) => { setPendingAccountApp(data); setTab('mykyc') }} />
        )}
        {isCustomer && activeKey === 'mykyc' && (
          <MyKycTab custId={session.custId} authHeader={authHeader} pendingAccountApp={pendingAccountApp} />
        )}
        {isAdmin && activeKey === 'admin' && <AdminTab authHeader={authHeader} />}
        {isCustomer && activeKey === 'myaccounts' && <MyAccountsTab custId={session.custId} authHeader={authHeader} />}
        {isCustomer && activeKey === 'mytransactions' && <MyTransactionsTab authHeader={authHeader} />}
        {isCustomer && activeKey === 'myloans' && <MyLoansTab custId={session.custId} authHeader={authHeader} />}
        {isCustomer && activeKey === 'mypayments' && <MyPaymentsTab custId={session.custId} authHeader={authHeader} />}
        {isCustomer && activeKey === 'notifications' && <NotificationsTab custId={session.custId} authHeader={authHeader} />}
      </div>
    </div>
  )
}
