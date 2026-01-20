import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  ResponsiveContainer,
} from "recharts";
import styles from "./Statistics.module.css";

export default function MetricChart({ title, data, dataKey }) {
  return (
    <div className={styles.chartCard}>
      <h4 className={styles.chartTitle}>{title}</h4>

      <div className={styles.chartWrapper}>
        <ResponsiveContainer width="100%" height="100%">
          <LineChart data={data} className={styles.chart}>
            <CartesianGrid strokeDasharray="3 3" className={styles.grid}/>
            <XAxis dataKey="date" tick={{ className: styles.axisLabel }}/>
            <YAxis domain={[0, 10]} tick={{ className: styles.axisLabel }}/>
            <Line
                type="monotone"
                dataKey={dataKey}
                className={styles.line} 
                dot={{ r: 4 }}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}