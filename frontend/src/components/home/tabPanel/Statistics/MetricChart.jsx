import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
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
          <LineChart data={data}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="date" />
            <YAxis domain={[0, 5]} />
            <Tooltip cursor={false} />
            <Line
              type="monotone"
              dataKey={dataKey}
              stroke="var(--text-accent)"
              strokeWidth={3}
              dot={{ r: 4 }}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}