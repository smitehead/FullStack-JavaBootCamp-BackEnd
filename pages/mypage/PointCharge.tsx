import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { CURRENT_USER } from '../../services/mockData';
import { Landmark, ArrowRight, CheckCircle2, ArrowLeft, X, CreditCard } from 'lucide-react';

const MOCK_PAYMENT_METHODS = [
  { id: 'pm_1', type: 'card', provider: '현대카드', number: '1234-****-****-5678', isDefault: true },
  { id: 'pm_2', type: 'card', provider: '삼성카드', number: '9876-****-****-4321', isDefault: false },
  { id: 'pm_3', type: 'account', provider: '신한은행', number: '110-123-456789', isDefault: false },
];

export const PointCharge: React.FC = () => {
  const navigate = useNavigate();
  const [amount, setAmount] = useState<string>('');
  const [isSuccess, setIsSuccess] = useState(false);
  
  // Payment Method State
  const [paymentMethods] = useState(MOCK_PAYMENT_METHODS);
  const [selectedMethodId, setSelectedMethodId] = useState('pm_1');
  const [isMethodModalOpen, setIsMethodModalOpen] = useState(false);

  const selectedMethod = paymentMethods.find(m => m.id === selectedMethodId) || paymentMethods[0];

  const handleCharge = () => {
    const numAmount = Number(amount);
    if (!numAmount || isNaN(numAmount) || numAmount <= 0) return;
    setIsSuccess(true);
    // Removed automatic navigation bug
  };

  if (isSuccess) {
    return (
      <div className="max-w-md mx-auto px-6 py-20 text-center animate-in fade-in zoom-in duration-500">
        <div className="w-24 h-24 bg-emerald-50 rounded-full flex items-center justify-center mx-auto mb-8">
          <CheckCircle2 className="w-12 h-12 text-emerald-500" />
        </div>
        <h2 className="text-3xl font-black text-gray-900 mb-4">충전 완료!</h2>
        <p className="text-gray-500 font-medium mb-10">
          {Number(amount).toLocaleString()}P가 성공적으로 충전되었습니다.
        </p>
        <div className="bg-gray-50 rounded-3xl p-6 mb-10">
          <div className="flex justify-between items-center mb-3">
            <span className="text-sm text-gray-400 font-bold">현재 잔액</span>
            <span className="text-xl font-black text-gray-900">
              {(CURRENT_USER.points + Number(amount)).toLocaleString()}P
            </span>
          </div>
        </div>
        <button 
          onClick={() => navigate('/points')}
          className="w-full py-4 bg-gray-900 text-white font-black rounded-2xl hover:bg-black transition-all shadow-xl"
        >
          확인
        </button>
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto px-6 py-12">
      <div className="flex items-center gap-4 mb-10">
        <button 
          onClick={() => navigate(-1)}
          className="p-2 hover:bg-gray-100 rounded-full transition-colors"
        >
          <ArrowLeft className="w-6 h-6 text-gray-900" />
        </button>
        <h2 className="text-3xl font-black text-gray-900 tracking-normal">포인트 충전</h2>
      </div>

      <div className="space-y-8">
        {/* Payment Method */}
        <div className="bg-white rounded-3xl border border-gray-100 p-6 shadow-sm">
          <p className="text-xs font-bold text-gray-400 uppercase tracking-wider mb-4">결제 수단</p>
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <div className="w-12 h-12 bg-gray-50 rounded-2xl flex items-center justify-center border border-gray-100">
                {selectedMethod.type === 'card' ? <CreditCard className="w-6 h-6 text-indigo-600" /> : <Landmark className="w-6 h-6 text-gray-400" />}
              </div>
              <div>
                <p className="font-black text-gray-900">{selectedMethod.provider}</p>
                <p className="text-sm text-gray-400 font-medium">{selectedMethod.number}</p>
              </div>
            </div>
            <button 
              onClick={() => setIsMethodModalOpen(true)}
              className="px-4 py-2 bg-gray-50 text-indigo-600 text-xs font-bold rounded-xl hover:bg-indigo-50 transition-all"
            >
              변경
            </button>
          </div>
        </div>

        {/* Amount Input */}
        <div className="bg-white rounded-3xl border border-gray-100 p-8 shadow-sm text-center">
          <h4 className="text-xl font-black text-gray-900 mb-8">얼마를 충전할까요?</h4>
          
          <div className="flex flex-col items-center mb-10">
            <div className="relative w-full max-w-[240px]">
              <input 
                type="text" 
                value={amount}
                onChange={(e) => setAmount(e.target.value.replace(/[^0-9]/g, ''))}
                placeholder="0"
                className="w-full text-4xl font-black py-4 border-b-4 border-gray-100 focus:border-indigo-600 outline-none transition-all placeholder:text-gray-200 text-center pr-8"
              />
              <span className="absolute right-0 bottom-5 text-2xl font-black text-gray-300">원</span>
            </div>

            {/* Balance after recharge */}
            {amount && Number(amount) > 0 && (
              <p className="mt-4 text-sm font-bold text-gray-400">
                충전 후 잔액 <span className="text-indigo-600">{(CURRENT_USER.points + Number(amount)).toLocaleString()}P</span>
              </p>
            )}
          </div>

          <div className="grid grid-cols-4 gap-2 mb-8">
            {['10000', '30000', '50000', '100000'].map(val => (
              <button 
                key={val}
                onClick={() => setAmount(prev => (Number(prev) + Number(val)).toString())}
                className="py-2.5 bg-gray-50 hover:bg-gray-100 text-gray-600 text-xs font-bold rounded-xl transition-all"
              >
                {Number(val).toLocaleString()}
              </button>
            ))}
          </div>

          <button 
            onClick={handleCharge}
            disabled={!amount || Number(amount) <= 0}
            className="w-full py-4 bg-indigo-600 text-white font-black rounded-2xl hover:bg-indigo-700 transition-all shadow-xl shadow-indigo-100 disabled:opacity-50 disabled:shadow-none"
          >
            충전하기
          </button>
        </div>
      </div>

      {/* Payment Method Modal */}
      {isMethodModalOpen && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={() => setIsMethodModalOpen(false)}></div>
          <div className="relative bg-white w-full max-w-lg rounded-[32px] shadow-2xl overflow-hidden animate-in zoom-in-95 duration-300">
            <div className="p-8 border-b border-gray-50 flex items-center justify-between">
              <h3 className="text-xl font-bold text-gray-900">결제 수단 관리</h3>
              <div className="flex items-center gap-4">
                <button 
                  onClick={() => alert('미구현 기능입니다.')}
                  className="px-4 py-2 bg-gray-900 text-white text-xs font-bold rounded-xl hover:bg-black transition-all"
                >
                  추가하기
                </button>
                <button onClick={() => setIsMethodModalOpen(false)} className="p-2 text-gray-400 hover:text-gray-600 transition-colors">
                  <X className="w-6 h-6" />
                </button>
              </div>
            </div>
            
            <div className="p-8 max-h-[60vh] overflow-y-auto">
              <div className="space-y-4">
                {paymentMethods.map(method => (
                  <div 
                    key={method.id}
                    onClick={() => {
                      setSelectedMethodId(method.id);
                      setIsMethodModalOpen(false);
                    }}
                    className={`flex items-center justify-between p-5 rounded-2xl border transition-all cursor-pointer ${selectedMethodId === method.id ? 'border-indigo-600 bg-indigo-50/50' : 'border-gray-100 bg-white hover:border-gray-200'}`}
                  >
                    <div className="flex items-center gap-4">
                      <div className={`w-10 h-10 rounded-xl flex items-center justify-center ${selectedMethodId === method.id ? 'bg-indigo-600 text-white' : 'bg-gray-100 text-gray-400'}`}>
                        {method.type === 'card' ? <CreditCard className="w-5 h-5" /> : <Landmark className="w-5 h-5" />}
                      </div>
                      <div>
                        <p className="font-bold text-gray-900">{method.provider}</p>
                        <p className="text-xs text-gray-400 font-medium">{method.number}</p>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
