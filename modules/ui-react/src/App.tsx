import { BrowserRouter, Routes, Route } from 'react-router-dom';
import IncidentGraph from './components/graph/IncidentGraph';

function Home() {
  return (
    <div>
      <h1>MagicOnCall</h1>
      <p>AI-Assisted On-Call Operating System</p>
    </div>
  );
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/incidents/:incidentId/graph" element={<IncidentGraph />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
