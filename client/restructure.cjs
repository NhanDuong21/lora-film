const fs = require('fs');
const path = require('path');

const src = 'src/features';
function mkdir(p) { if (!fs.existsSync(p)) fs.mkdirSync(p, { recursive: true }); }
function mv(f, t) { if (fs.existsSync(f)) fs.renameSync(f, t); }

// concessions-sales
mkdir(src + '/concessions-sales/admin/pages');
mkdir(src + '/concessions-sales/employee/pages');

mv(src + '/concessions-sales/pages/AdminConcessionInventoryPage.jsx', src + '/concessions-sales/admin/pages/AdminConcessionInventoryPage.jsx');
mv(src + '/concessions-sales/pages/AdminConcessionSalesPage.jsx', src + '/concessions-sales/admin/pages/AdminConcessionSalesPage.jsx');
mv(src + '/concessions-sales/pages/EmployeePOSPage.jsx', src + '/concessions-sales/employee/pages/EmployeePOSPage.jsx');

if (fs.existsSync(src + '/concessions-sales/pages')) fs.rmdirSync(src + '/concessions-sales/pages');

fs.writeFileSync(src + '/concessions-sales/admin/routes.jsx', `import AdminConcessionInventory from './pages/AdminConcessionInventoryPage';
import AdminConcessionSalesPage from './pages/AdminConcessionSalesPage';

export const adminConcessionRoutes = [
    { path: 'concessions', element: <AdminConcessionInventory /> },
    { path: 'concession-sales', element: <AdminConcessionSalesPage /> }
];
`);

fs.writeFileSync(src + '/concessions-sales/employee/routes.jsx', `import EmployeePOSView from './pages/EmployeePOSPage';

export const employeeConcessionRoutes = [
    { index: true, element: <EmployeePOSView /> },
    { path: 'pos', element: <EmployeePOSView /> }
];
`);

// internal-staff
mkdir(src + '/internal-staff/admin/pages');
mkdir(src + '/internal-staff/employee/pages');

if (fs.existsSync(src + '/internal-staff/pages')) {
    const staffPages = fs.readdirSync(src + '/internal-staff/pages');
    staffPages.forEach(file => {
        if (file.includes('Employee')) {
            mv(src + '/internal-staff/pages/' + file, src + '/internal-staff/employee/pages/' + file);
        } else {
            mv(src + '/internal-staff/pages/' + file, src + '/internal-staff/admin/pages/' + file);
        }
    });
    fs.rmdirSync(src + '/internal-staff/pages');
}

fs.writeFileSync(src + '/internal-staff/admin/routes.jsx', `import AdminDashboardView from './pages/AdminDashboardPage';
import AdminEventView from './pages/AdminEventPage';
import AdminFinanceView from './pages/AdminFinancePage';
import AdminMembersView from './pages/AdminMembersPage';
import AdminSettingsView from './pages/AdminSettingsPage';
import AdminStaffView from './pages/AdminStaffPage';
import AdminPayrollPage from './pages/AdminPayrollPage';

export const adminStaffRoutes = [
    { index: true, element: <AdminDashboardView /> },
    { path: 'events', element: <AdminEventView /> },
    { path: 'finance', element: <AdminFinanceView /> },
    { path: 'members', element: <AdminMembersView /> },
    { path: 'settings', element: <AdminSettingsView /> },
    { path: 'staff', element: <AdminStaffView /> },
    { path: 'payroll', element: <AdminPayrollPage /> }
];
`);

fs.writeFileSync(src + '/internal-staff/employee/routes.jsx', `import EmployeeCheckInView from './pages/EmployeeCheckInPage';
import EmployeeScheduleView from './pages/EmployeeSchedulePage';

export const employeeStaffRoutes = [
    { path: 'checkin', element: <EmployeeCheckInView /> },
    { path: 'schedules', element: <EmployeeScheduleView /> }
];
`);
