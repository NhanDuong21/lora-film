import { useEffect } from "react";
import Header from "../../components/layout/Header";
import Footer from "../../components/layout/Footer";
import HeroSection from "../../components/home/HeroSection";
import MovieSection from "../../components/home/MovieSection";
import EventSection from "../../components/home/EventSection";
import BookingStepsSection from "../../components/home/BookingStepsSection";
import InfoSection from "../../components/home/InfoSection";

export default function Home() {
    useEffect(() => {
        document.title = "LoraFilm - He thong dat ve xem phim truc tuyen";
    }, []);

    return (
        <div className="flex flex-col min-h-screen bg-brand-dark text-white selection:bg-brand-coral selection:text-white">
            {/* Main Sticky Header */}
            <Header />

            {/* Layout Wrapper */}
            <main className="flex-grow">
                {/* Hero Showcase Section */}
                <HeroSection />

                {/* Movie Section Placeholder Grid */}
                <MovieSection />

                {/* Event Section Placeholder Grid */}
                <EventSection />

                {/* Booking Steps Section */}
                <BookingStepsSection />

                {/* General Brand Information Section */}
                <InfoSection />
            </main>

            {/* Bottom Footer Section */}
            <Footer />
        </div>
    );
}
