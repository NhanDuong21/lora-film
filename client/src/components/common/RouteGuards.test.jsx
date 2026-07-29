import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { PermissionRoute, RoleRoute } from './RouteGuards';
import { useAuth } from '@/contexts/AuthContext';

vi.mock('@/contexts/AuthContext', () => ({
    useAuth: vi.fn()
}));

const renderAt = (element, initialEntry = '/protected') => render(
    <MemoryRouter initialEntries={[initialEntry]}>
        <Routes>
            <Route path="/login" element={<div>Login page</div>} />
            <Route path="/403" element={<div>Forbidden page</div>} />
            <Route path="/protected" element={element} />
        </Routes>
    </MemoryRouter>
);

describe('route authorization guards', () => {
    beforeEach(() => {
        useAuth.mockReset();
    });

    it('redirects an unauthenticated role request to login', () => {
        useAuth.mockReturnValue({
            isAuthenticated: false,
            isInitializing: false,
            userRole: null
        });

        renderAt(
            <RoleRoute allowedRoles={['ADMIN']}>
                <div>Protected content</div>
            </RoleRoute>
        );

        expect(screen.getByText('Login page')).toBeInTheDocument();
    });

    it('rejects an authenticated user with the wrong role', () => {
        useAuth.mockReturnValue({
            isAuthenticated: true,
            isInitializing: false,
            userRole: 'EMPLOYEE'
        });

        renderAt(
            <RoleRoute allowedRoles={['ADMIN']}>
                <div>Protected content</div>
            </RoleRoute>
        );

        expect(screen.getByText('Forbidden page')).toBeInTheDocument();
    });

    it('accepts ROLE_ prefixed role claims', () => {
        useAuth.mockReturnValue({
            isAuthenticated: true,
            isInitializing: false,
            userRole: 'ROLE_ADMIN'
        });

        renderAt(
            <RoleRoute allowedRoles={['ADMIN']}>
                <div>Protected content</div>
            </RoleRoute>
        );

        expect(screen.getByText('Protected content')).toBeInTheDocument();
    });

    it('rejects a user missing the required permission', () => {
        useAuth.mockReturnValue({
            isAuthenticated: true,
            isInitializing: false,
            user: { permissions: ['CUSTOMER_VIEW'] }
        });

        renderAt(
            <PermissionRoute requiredPermissions={['EMPLOYEE_VIEW']}>
                <div>Permission content</div>
            </PermissionRoute>
        );

        expect(screen.getByText('Forbidden page')).toBeInTheDocument();
    });

    it('accepts any matching permission by default', () => {
        useAuth.mockReturnValue({
            isAuthenticated: true,
            isInitializing: false,
            user: { permissions: ['EMPLOYEE_VIEW'] }
        });

        renderAt(
            <PermissionRoute requiredPermissions={['CUSTOMER_VIEW', 'EMPLOYEE_VIEW']}>
                <div>Permission content</div>
            </PermissionRoute>
        );

        expect(screen.getByText('Permission content')).toBeInTheDocument();
    });

    it('requires every permission when requireAll is enabled', () => {
        useAuth.mockReturnValue({
            isAuthenticated: true,
            isInitializing: false,
            user: { permissions: ['EMPLOYEE_VIEW'] }
        });

        renderAt(
            <PermissionRoute
                requiredPermissions={['EMPLOYEE_VIEW', 'EMPLOYEE_UPDATE']}
                requireAll
            >
                <div>Permission content</div>
            </PermissionRoute>
        );

        expect(screen.getByText('Forbidden page')).toBeInTheDocument();
    });

    it('allows the existing root-access override', () => {
        useAuth.mockReturnValue({
            isAuthenticated: true,
            isInitializing: false,
            user: { permissions: ['PERM_ROOT_ACCESS'] }
        });

        renderAt(
            <PermissionRoute requiredPermissions={['SYSTEM_CONFIGURATION']}>
                <div>Permission content</div>
            </PermissionRoute>
        );

        expect(screen.getByText('Permission content')).toBeInTheDocument();
    });

    it('allows the built-in administrator role without granular permission claims', () => {
        useAuth.mockReturnValue({
            isAuthenticated: true,
            isInitializing: false,
            user: { role: 'ROLE_ADMIN', permissions: [] }
        });

        renderAt(
            <PermissionRoute requiredPermissions={['SYSTEM_CONFIGURATION']}>
                <div>Permission content</div>
            </PermissionRoute>
        );

        expect(screen.getByText('Permission content')).toBeInTheDocument();
    });
});
