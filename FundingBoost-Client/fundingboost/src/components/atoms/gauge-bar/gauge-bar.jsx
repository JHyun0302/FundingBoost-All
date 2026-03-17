import ProgressBar from 'react-bootstrap/ProgressBar';
import './gauge-bar.scss';

function WithLabelExample({ value }) {

    return <ProgressBar variant="success" now={ value } label={`${ value }%`} className="gauge-bar" />;
}

export default WithLabelExample;