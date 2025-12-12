import { FileEdit, Send, Eye, CheckCircle, AlertCircle, XCircle } from "lucide-react";

const StatusBadge = ({ status }) => {
  const statusConfig = {
    DRAFT: { color: 'secondary', icon: FileEdit },
    SENT: { color: 'info', icon: Send },
    VIEWED: { color: 'primary', icon: Eye },
    PAID: { color: 'success', icon: CheckCircle },
    OVERDUE: { color: 'danger', icon: AlertCircle },
    CANCELLED: { color: 'dark', icon: XCircle }
  };
  
  const config = statusConfig[status] || statusConfig.DRAFT;
  const Icon = config.icon;
  
  return (
    <span className={`badge bg-${config.color} d-inline-flex align-items-center gap-1`}>
      <Icon size={14} />
      {status}
    </span>
  );
};

export default StatusBadge;
