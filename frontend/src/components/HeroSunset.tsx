import { Link } from 'react-router-dom'

export default function HeroSunset() {
  return (
    <section className="rounded-2xl p-10 bg-sunset-hero text-white shadow-xl">
      <h1 className="text-4xl font-bold mb-3">Plan trips & big purchases responsibly</h1>
      <p className="opacity-90 mb-6">
        Turn dream trips and big purchases into a doable plan—see if, when, and
        how you can afford them without blowing your budget.
      </p>
      <div className="flex gap-3">
        <Link to="/plan">
          <button className="px-5 py-3 rounded-xl bg-white text-primary font-semibold hover:bg-white/90 transition-colors">
            Plan a Goal
          </button>
        </Link>
        <Link to="/goals">
          <button className="px-5 py-3 rounded-xl bg-white/20 text-white font-semibold hover:bg-white/30 transition-colors border border-white/30">
            Your Goals
          </button>
        </Link>
      </div>
    </section>
  );
}