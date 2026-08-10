import HeroSection from "@/features/catalog/customer/components/home/HeroSection";
import MovieSection from "@/features/catalog/customer/components/home/MovieSection";
import EventSection from "@/features/catalog/customer/components/home/EventSection";
import BookingStepsSection from "@/features/catalog/customer/components/home/BookingStepsSection";
import InfoSection from "@/features/catalog/customer/components/home/InfoSection";

export default function Home() {
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
