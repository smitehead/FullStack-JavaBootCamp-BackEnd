import React from 'react';
import { Toaster } from 'sonner';
import { BrowserRouter, Routes, Route } from 'react-router-dom';

// [ Context & Components ]
import { AppProvider } from '@/context/AppContext';
import { Layout } from '@/components/Layout';

// [ Root Pages ]
import { Home } from '@/pages/Home';
import { AboutUs } from '@/pages/AboutUs';

// [ Auth Pages (인증/회원) ]
import { Login } from '@/pages/auth/Login';
import { Signup } from '@/pages/auth/Signup';
import { FindAccount } from '@/pages/auth/FindAccount';

// [ Product Pages (상품/경매) ]
import { ProductList } from '@/pages/product/ProductList';
import { ProductDetail } from '@/pages/product/ProductDetail';
import { ProductRegister } from '@/pages/product/ProductRegister';
import { WonProductDetail } from '@/pages/product/WonProductDetail';

// [ MyPage Pages (마이페이지/재화) ]
import { MyPage } from '@/pages/mypage/MyPage';
import { Settings } from '@/pages/mypage/Settings';
import { Points } from '@/pages/mypage/Points';
import { PointCharge } from '@/pages/mypage/PointCharge';
import { PointWithdraw } from '@/pages/mypage/PointWithdraw';

// [ Trade Pages (거래/커뮤니케이션) ]
import { Inbox } from '@/pages/trade/Inbox';
import { SellerProfile } from '@/pages/trade/SellerProfile';

// [ CS Pages (고객센터) ]
import { FAQ } from '@/pages/cs/FAQ';
import { NoticeList } from '@/pages/cs/NoticeList';
import { NoticeDetail } from '@/pages/cs/NoticeDetail';
import { InquiryList } from '@/pages/cs/InquiryList';
import { InquiryCreate } from '@/pages/cs/InquiryCreate';
import { InquiryDetail } from '@/pages/cs/InquiryDetail';
import { Report } from '@/pages/cs/Report';

// [ Admin Pages (관리자) ]
import { AdminLayout } from '@/pages/admin/AdminLayout';
import { AdminDashboard } from '@/pages/admin/AdminDashboard';
import { UserManagement } from '@/pages/admin/UserManagement';
import { NoticeManagement } from '@/pages/admin/NoticeManagement';
import { InquiryManagement } from '@/pages/admin/InquiryManagement';
import { BannerManagement } from '@/pages/admin/BannerManagement';
import { NotificationManagement } from '@/pages/admin/NotificationManagement';
import { AuctionManagement } from '@/pages/admin/AuctionManagement';
import { ReportManagement } from '@/pages/admin/ReportManagement';
import { ActivityLogManagement } from '@/pages/admin/ActivityLogManagement';
import { MannerHistoryManagement } from '@/pages/admin/MannerHistoryManagement';

const App: React.FC = () => {
  return (
    <BrowserRouter>
      <AppProvider>
        <Toaster 
          position="top-center" 
          expand={false} 
          visibleToasts={5} 
          gap={12} 
          richColors 
        />
        <Routes>
          {/* Admin Routes */}
          <Route path="/admin/*" element={
            <AdminLayout>
              <Routes>
                <Route path="/" element={<AdminDashboard />} />
                <Route path="/users" element={<UserManagement />} />
                <Route path="/notices" element={<NoticeManagement />} />
                <Route path="/inquiries" element={<InquiryManagement />} />
                <Route path="/banners" element={<BannerManagement />} />
                <Route path="/notifications" element={<NotificationManagement />} />
                <Route path="/auctions" element={<AuctionManagement />} />
                <Route path="/reports" element={<ReportManagement />} />
                <Route path="/activity-logs" element={<ActivityLogManagement />} />
                <Route path="/manner-history" element={<MannerHistoryManagement />} />
              </Routes>
            </AdminLayout>
          } />

          {/* User Routes */}
          <Route path="/*" element={
            <Layout>
              <Routes>
                <Route path="/" element={<Home />} />
                <Route path="/search" element={<ProductList />} />
                <Route path="/products/:id" element={<ProductDetail />} />
                <Route path="/won/:id" element={<WonProductDetail />} />
                <Route path="/inbox" element={<Inbox />} />
                <Route path="/about" element={<AboutUs />} />
                <Route path="/notice" element={<NoticeList />} />
                <Route path="/notice/:id" element={<NoticeDetail />} />
                <Route path="/inquiry" element={<InquiryList />} />
                <Route path="/inquiry/create" element={<InquiryCreate />} />
                <Route path="/inquiry/:id" element={<InquiryDetail />} />
                <Route path="/register" element={<ProductRegister />} />
                <Route path="/mypage" element={<MyPage />} />
                <Route path="/seller/:id" element={<SellerProfile />} />
                <Route path="/settings" element={<Settings />} />
                <Route path="/points" element={<Points />} />
                <Route path="/points/charge" element={<PointCharge />} />
                <Route path="/points/withdraw" element={<PointWithdraw />} />
                <Route path="/login" element={<Login />} />
                <Route path="/signup" element={<Signup />} />
                <Route path="/report" element={<Report />} />
                <Route path="/find-id" element={<FindAccount />} />
                <Route path="/find-pw" element={<FindAccount />} />
                <Route path="/faq" element={<FAQ />} />
              </Routes>
            </Layout>
          } />
        </Routes>
      </AppProvider>
    </BrowserRouter>
  );
};

export default App;