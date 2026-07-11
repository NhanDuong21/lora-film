import { Outlet } from 'react-router-dom';
import Header from './Header';
import Footer from './Footer';

export default function MainLayout() {
  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-100 flex flex-col selection:bg-brand-orange selection:text-white">
      {/* Dynamic sticky header */}
      <Header />

      {/* Main Content Sections */}
      <main className="flex-grow pt-20">
        <Outlet />
      </main>

      {/* Sleek Dark Footer */}
      <Footer />
    </div>
  );
}
