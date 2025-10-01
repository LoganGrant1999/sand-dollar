import { Link } from 'react-router-dom'
import { ArrowRight, CreditCard, PieChart, Heart, LogIn, UserPlus } from 'lucide-react'
import { Button } from '@/components/ui/button'
import circleLogo from '@/assets/circle_logo.png'
import transparentLogo from '@/assets/transparent_logo.png'
import missionCollage from '@/assets/MISSION_COLLAGE.jpg'
import travelImage from '@/assets/travel_image.jpg'
import diningImage from '@/assets/dining_image.jpg'
import shoppingImage from '@/assets/shopping_image.jpg'
import wellnessImage from '@/assets/wellness_image.jpg'

export default function Landing() {
  return (
    <div className="min-h-screen w-full bg-[var(--color-bg-dark)]">
      {/* Navigation Header */}
      <nav className="fixed top-0 left-0 right-0 z-50 flex justify-between items-center px-6 py-4 bg-white/95 backdrop-blur-sm border-b border-border shadow-sm">
        <div className="flex items-center gap-3">
          <img
            src={circleLogo}
            alt="Sand Dollar Logo"
            className="h-10 w-10 object-cover rounded-full"
          />
          <div className="gradient-text text-xl font-bold">
            Sand Dollar
          </div>
        </div>
        <div className="flex gap-4">
          <Link to="/login">
            <Button variant="outline" className="flex items-center gap-2">
              <LogIn className="h-4 w-4" />
              Sign In
            </Button>
          </Link>
          <Link to="/register">
            <Button className="flex items-center gap-2">
              <UserPlus className="h-4 w-4" />
              Get Started Today
            </Button>
          </Link>
        </div>
      </nav>

      {/* Hero Section */}
      <section className="relative w-screen ml-[50%] -translate-x-1/2 flex items-center justify-center py-20 pt-32 bg-gradient-to-br from-blue-100 via-teal-100 to-orange-100">
        <div className="w-full text-center">
          <h1 className="text-5xl md:text-7xl font-bold mb-6 bg-gradient-to-r from-[var(--color-accent-blue)] to-[var(--color-accent-teal)] bg-clip-text text-transparent leading-tight py-2">
            Enjoy Life. Spend Smarter.
          </h1>
          <p className="text-xl md:text-2xl text-[var(--color-text-secondary)] mb-8 leading-relaxed max-w-3xl mx-auto">
            Sand Dollar helps you budget responsibly so you can say yes to experiences and luxuries without the guilt.
          </p>
          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <Link to="/register">
              <Button size="lg" className="px-8 py-4 text-lg">
                Get Started Today
                <ArrowRight className="ml-2 h-5 w-5" />
              </Button>
            </Link>
            <Link to="/login">
              <Button variant="outline" size="lg" className="px-8 py-4 text-lg">
                Sign In
              </Button>
            </Link>
          </div>
        </div>
      </section>

      {/* Mission Statement Section */}
      <section className="w-screen ml-[50%] -translate-x-1/2 py-20">
        <div className="w-full px-4">
          <div className="grid md:grid-cols-2 gap-12 items-center">
            <div>
              <h2 className="text-4xl md:text-5xl font-bold mb-6 text-[var(--color-text-primary)]">
                Money isn't just bills.
              </h2>
              <p className="text-xl text-[var(--color-text-secondary)] leading-relaxed">
                It's dinners, trips, and peace of mind. Sand Dollar empowers you to enjoy your money while staying in control.
              </p>
            </div>
            <div className="relative">
              <img
                src={missionCollage}
                alt="Lifestyle collage showing experiences and financial freedom"
                className="aspect-square object-cover w-full shadow-lg"
              />
            </div>
          </div>
        </div>
      </section>

      {/* How It Works Section */}
      <section className="w-screen ml-[50%] -translate-x-1/2 py-20 bg-[var(--color-sand-neutral)]">
        <div className="w-full px-4">
          <h2 className="text-4xl md:text-5xl font-bold text-center mb-16 text-[var(--color-text-primary)]">
            How It Works
          </h2>
          <div className="grid md:grid-cols-3 gap-8">
            <div className="text-center group">
              <div className="w-20 h-20 mx-auto mb-6 bg-gradient-to-br from-primary to-secondary rounded-2xl flex items-center justify-center group-hover:scale-110 transition-transform">
                <CreditCard className="h-10 w-10 text-white" />
              </div>
              <h3 className="text-2xl font-bold mb-4 text-[var(--color-text-primary)]">Connect your accounts</h3>
              <p className="text-[var(--color-text-secondary)]">
                Securely link your bank accounts and credit cards to get a complete picture of your finances.
              </p>
            </div>

            <div className="text-center group">
              <div className="w-20 h-20 mx-auto mb-6 bg-gradient-to-br from-primary to-secondary rounded-2xl flex items-center justify-center group-hover:scale-110 transition-transform">
                <PieChart className="h-10 w-10 text-white" />
              </div>
              <h3 className="text-2xl font-bold mb-4 text-[var(--color-text-primary)]">Get a budget built around your lifestyle</h3>
              <p className="text-[var(--color-text-secondary)]">
                Our smart algorithm creates a personalized budget that prioritizes what matters most to you.
              </p>
            </div>

            <div className="text-center group">
              <div className="w-20 h-20 mx-auto mb-6 bg-gradient-to-br from-primary to-secondary rounded-2xl flex items-center justify-center group-hover:scale-110 transition-transform">
                <Heart className="h-10 w-10 text-white" />
              </div>
              <h3 className="text-2xl font-bold mb-4 text-[var(--color-text-primary)]">See how much you can spend on what you love</h3>
              <p className="text-[var(--color-text-secondary)]">
                Know exactly how much you can afford to spend on experiences, dining, and treats guilt-free.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Lifestyle Section */}
      <section className="w-full py-20">
        <div className="w-full px-4">
          <div className="text-center mb-16">
            <h2 className="text-4xl md:text-5xl font-bold mb-6 text-[var(--color-text-primary)]">
              Your budget shouldn't stop you from living
            </h2>
            <p className="text-2xl text-[var(--color-text-secondary)]">
              — it should make living better.
            </p>
          </div>

          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <img
              src={travelImage}
              alt="Travel"
              className="aspect-square object-cover object-center-left rounded-xl hover:scale-105 transition-transform shadow-lg"
            />

            <img
              src={diningImage}
              alt="Dining"
              className="aspect-square object-cover object-center rounded-xl hover:scale-105 transition-transform shadow-lg"
            />

            <img
              src={shoppingImage}
              alt="Shopping"
              className="aspect-square object-cover object-center rounded-xl hover:scale-105 transition-transform shadow-lg"
            />

            <img
              src={wellnessImage}
              alt="Wellness"
              className="aspect-square object-cover object-center rounded-xl hover:scale-105 transition-transform shadow-lg"
            />
          </div>
        </div>
      </section>


      {/* Call-to-Action Banner */}
      <section className="w-screen ml-[50%] -translate-x-1/2 py-20 bg-[var(--color-sand-neutral)]">
        <div className="w-full text-center px-4">
          <h2 className="text-4xl md:text-6xl font-bold mb-6 text-[var(--color-text-primary)]">
            Start spending smarter today.
          </h2>
          <p className="text-xl text-[var(--color-text-secondary)] mb-8">
            Join users who've transformed their relationship with money.
          </p>
          <Link to="/register">
            <Button size="lg" className="bg-primary hover:bg-primary/90 text-white font-semibold px-12 py-4 text-xl">
              Sign up today
              <ArrowRight className="ml-3 h-6 w-6" />
            </Button>
          </Link>
        </div>
      </section>

      {/* Footer */}
      <footer className="w-screen ml-[50%] -translate-x-1/2 py-16 bg-[var(--color-bg-dark)] border-t border-border" style={{backgroundColor: '#1a1a1a'}}>
        <div className="w-full text-center px-4">
          <div className="mb-8">
            <img
              src={circleLogo}
              alt="Sand Dollar Logo"
              className="h-12 mx-auto object-contain"
            />
            <p className="text-white leading-relaxed max-w-md mx-auto" style={{color: '#ffffff'}}>
              Built for people who believe money is a tool to live, not just survive.
            </p>
          </div>

          <div className="border-t border-white/20 pt-8">
            <p className="text-white/80" style={{color: '#ffffff'}}>
              © 2025 Sand Dollar. All rights reserved.
            </p>
          </div>
        </div>
      </footer>
    </div>
  )
}