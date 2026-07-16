import { useEffect } from "react";
import HeroSection from "@/features/movies-genres/components/home/HeroSection";
import MovieSection from "@/features/movies-genres/components/home/MovieSection";
import EventSection from "@/features/movies-genres/components/home/EventSection";
import BookingStepsSection from "@/features/movies-genres/components/home/BookingStepsSection";
import InfoSection from "@/features/movies-genres/components/home/InfoSection";

export default function Home() {
    useEffect(() => {
        document.title = "LoraFilm - He thong dat ve xem phim truc tuyen";
    }, []);

    return (
        <div className="flex flex-col min-h-screen bg-brand-dark text-white selection:bg-brand-orange selection:text-white">
            <HeroSection />
            <MovieSection />
            <EventSection />
            <BookingStepsSection />
            <InfoSection />
        </div>
    );
}
