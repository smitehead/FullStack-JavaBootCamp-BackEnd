import React from 'react';
import { toast } from 'sonner';
import { motion } from 'motion/react';
import { CheckCircle2, AlertCircle, Info, Sparkles } from 'lucide-react';

export type ToastType = 'success' | 'error' | 'info' | 'warning' | 'bid';

export const showToast = (message: string, type: ToastType = 'info') => {
  const config = {
    success: { icon: <CheckCircle2 className="w-4 h-4 text-emerald-500" />, color: 'text-emerald-600' },
    error: { icon: <AlertCircle className="w-4 h-4 text-red-500" />, color: 'text-red-600' },
    info: { icon: <Info className="w-4 h-4 text-blue-500" />, color: 'text-blue-600' },
    warning: { icon: <AlertCircle className="w-4 h-4 text-amber-500" />, color: 'text-amber-600' },
    bid: { icon: <Sparkles className="w-4 h-4 text-red-500 animate-pulse" />, color: 'text-red-500' },
  };

  const { icon, color } = config[type];

  toast.custom((t) => (
    <motion.div
      initial={{ opacity: 0, y: -20, scale: 0.95 }}
      animate={{ opacity: 1, y: 0, scale: 1 }}
      exit={{ opacity: 0, scale: 0.95 }}
      className="bg-white border border-gray-100 shadow-2xl rounded-full py-3 px-6 flex items-center gap-3 relative min-w-[300px]"
    >
      <div className="flex items-center justify-center shrink-0">{icon}</div>
      <p className="text-sm font-black text-gray-900 leading-none whitespace-nowrap">
        {message.split("'").map((part, i) =>
          i % 2 === 1 ? <span key={i} className={color}>'{part}'</span> : part
        )}
      </p>
    </motion.div>
  ), { position: 'top-center', duration: 3000 });
};
