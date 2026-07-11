import AppRoutes from "./routes/AppRoutes";
import { AuthProvider } from "./contexts/AuthContext";
import { DataProvider } from "./contexts/DataContext";

function App() {
    return (
        <AuthProvider>
            <DataProvider>
                <AppRoutes />
            </DataProvider>
        </AuthProvider>
    );
}

export default App;