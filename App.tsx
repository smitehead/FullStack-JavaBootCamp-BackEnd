import React from 'react';
import { HashRouter, Routes, Route } from 'react-router-dom';
import { Layout } from './components/Layout';
import { Home } from './pages/Home';
import { ProductList } from './pages/ProductList';
import { ProductDetail } from './pages/ProductDetail';
import { ProductRegister } from './pages/ProductRegister';
import { MyPage } from './pages/MyPage';
import { SellerProfile } from './pages/SellerProfile';
import { Settings } from './pages/Settings';
import { Points } from './pages/Points';
import { PointCharge } from './pages/PointCharge';
import { PointWithdraw } from './pages/PointWithdraw';
import { Login } from './pages/Login';
import { Signup } from './pages/Signup';
import { Report } from './pages/Report';
import { FindAccount } from './pages/FindAccount';
import { WonProductDetail } from './pages/WonProductDetail';
import { FAQ } from './pages/FAQ';
import { Inbox } from './pages/Inbox';
import { NoticeList } from './pages/NoticeList';
import { NoticeDetail } from './pages/NoticeDetail';
import { InquiryList } from './pages/InquiryList';
import { InquiryCreate } from './pages/InquiryCreate';
import { InquiryDetail } from './pages/InquiryDetail';
import { CSCenter } from './pages/CSCenter';
import { AdminLayout } from './pages/admin/AdminLayout';
import { AdminDashboard } from './pages/admin/AdminDashboard';
import { UserManagement } from './pages/admin/UserManagement';
import { NoticeManagement } from './pages/admin/NoticeManagement';
import { InquiryManagement } from './pages/admin/InquiryManagement';
import { BannerManagement } from './pages/admin/BannerManagement';
import { NotificationManagement } from './pages/admin/NotificationManagement';
import { AuctionManagement } from './pages/admin/AuctionManagement';
import { ReportManagement } from './pages/admin/ReportManagement';
import { ActivityLogManagement } from './pages/admin/ActivityLogManagement';
import { MannerHistoryManagement } from './pages/admin/MannerHistoryManagement';

import { AppProvider } from './context/AppContext';

const App: React.FC = () => {
  return (
    <HashRouter>
      <AppProvider>
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
                <Route path="/cs" element={<CSCenter />} />
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
    </HashRouter>
  );
};

export default App;
