export default function BabyCareResourceNotFoundPage() {
  return (
    <main className="flex min-h-screen items-center justify-center bg-[#F6F1EC] p-6 font-sans text-[#5A463F]">
      <div role="alert" className="rounded-[32px] bg-white p-8 text-center shadow-[0_12px_32px_rgba(90,70,63,0.06)]">
        <h1 className="text-2xl font-black">Resource not found</h1>
        <p className="mt-3 text-base text-[#9C857C]">The requested baby resource is not available.</p>
      </div>
    </main>
  );
}
