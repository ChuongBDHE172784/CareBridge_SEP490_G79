import { useEffect, useState } from 'react';
import { BadgeCheck, FileSignature, HeartHandshake } from 'lucide-react';
import { acceptContract, chooseExpertType, getContractOffer } from '../services/expertApi';
import type { ContractOfferResponse, GrantableExpertType } from '../services/expertApi';

/** Cùng khung thẻ với các bước khác trong ExpertOnboardingPage. */
function StepCard({
	icon,
	title,
	description,
	children,
}: {
	icon: React.ReactNode;
	title: string;
	description: string;
	children: React.ReactNode;
}) {
	return (
		<section className="rounded-3xl border border-outline-variant/60 bg-surface p-6 sm:p-8 shadow-md">
			<div className="mb-6 flex items-start gap-4 pb-4 border-b border-outline-variant/40">
				<div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-primary-container text-primary shadow-xs">
					{icon}
				</div>
				<div>
					<h2 className="text-xl font-bold text-on-surface m-0 leading-tight">{title}</h2>
					<p className="mt-1 text-xs text-on-surface-variant leading-relaxed m-0">{description}</p>
				</div>
			</div>
			{children}
		</section>
	);
}

/**
 * Bước 2 — chọn hình thức hợp tác.
 *
 * Mỗi thẻ nói thẳng NGHĨA VỤ đi kèm chứ không chỉ mô tả quyền lợi: viết nghĩa vụ ngay trên
 * thẻ lọc bớt người chọn nhóm hợp tác chỉ vì huy hiệu đẹp hơn, và giảm số ca admin phải xếp
 * xuống nhóm cộng đồng.
 */
export function ExpertTypeStep({ onDone }: { onDone: () => Promise<void> }) {
	const [selected, setSelected] = useState<GrantableExpertType | null>(null);
	const [saving, setSaving] = useState(false);
	const [error, setError] = useState<string | null>(null);

	const submit = async () => {
		if (!selected || saving) return;
		setSaving(true);
		setError(null);
		try {
			await chooseExpertType(selected);
			await onDone();
		} catch {
			setError('Không lưu được lựa chọn. Vui lòng thử lại.');
			setSaving(false);
		}
	};

	const cards = [
		{
			value: 'PENDING_CONTRACT' as GrantableExpertType,
			icon: <BadgeCheck className="text-emerald-600" size={26} />,
			title: 'Chuyên gia Hệ thống',
			benefit:
				'Được ưu tiên giới thiệu tới người dùng cần tư vấn, và hiển thị huy hiệu Chuyên gia Hệ thống trên hồ sơ công khai.',
			obligation: 'Yêu cầu: ký Thoả thuận hợp tác và duy trì tối thiểu 10 ca rảnh mỗi tuần.',
			ring: 'border-emerald-500 bg-emerald-50/60',
		},
		{
			value: 'COMMUNITY' as GrantableExpertType,
			icon: <HeartHandshake className="text-sky-600" size={26} />,
			title: 'Chuyên gia Y tế Cộng đồng',
			benefit:
				'Tư vấn hỗ trợ cộng đồng theo khả năng của bạn, hiển thị chứng chỉ hành nghề đã được kiểm duyệt.',
			obligation: 'Không cần cam kết lịch cố định, phản hồi khi bạn rảnh.',
			ring: 'border-sky-500 bg-sky-50/60',
		},
	];

	return (
		<StepCard
			icon={<HeartHandshake />}
			title="Hình thức hợp tác"
			description="Cả hai hình thức đều phải qua cùng một quy trình xác minh danh tính và chứng chỉ hành nghề."
		>
			<div className="grid gap-4 sm:grid-cols-2">
				{cards.map((card) => (
					<button
						key={card.value}
						type="button"
						onClick={() => setSelected(card.value)}
						disabled={saving}
						className={`rounded-2xl border p-5 text-left transition-all ${
							selected === card.value
								? card.ring
								: 'border-outline-variant/50 bg-surface hover:border-outline'
						}`}
					>
						<div className="flex items-center gap-3">
							{card.icon}
							<span className="text-base font-bold text-on-surface">{card.title}</span>
						</div>
						<p className="mt-3 text-sm leading-6 text-on-surface-variant">{card.benefit}</p>
						<p className="mt-3 rounded-xl bg-surface-container-highest/60 px-3 py-2 text-xs font-semibold leading-5 text-outline">
							{card.obligation}
						</p>
					</button>
				))}
			</div>
			{error && <p className="mt-4 text-sm font-medium text-rose-600">{error}</p>}
			<button
				type="button"
				onClick={submit}
				disabled={!selected || saving}
				className="mt-6 inline-flex items-center gap-2 rounded-full bg-primary px-6 py-2.5 font-semibold text-on-primary disabled:opacity-40"
			>
				{saving ? 'Đang lưu…' : 'Tiếp tục'}
			</button>
		</StepCard>
	);
}

/**
 * Trang ký Thoả thuận hợp tác (click-wrap).
 *
 * Nút đồng ý chỉ bật sau khi cuộn hết toàn văn — đây là chi tiết làm nên giá trị pháp lý của
 * click-wrap, chứng minh người ký đã có cơ hội đọc. Checkbox không tick sẵn, và họ tên phải gõ
 * lại đúng như hồ sơ đã duyệt (server kiểm lại, bỏ qua dấu và hoa thường).
 */
export function ContractStep({ onDone }: { onDone: () => Promise<void> }) {
	const [offer, setOffer] = useState<ContractOfferResponse | null>(null);
	const [loading, setLoading] = useState(true);
	const [saving, setSaving] = useState(false);
	const [scrolledToEnd, setScrolledToEnd] = useState(false);
	const [agreed, setAgreed] = useState(false);
	const [fullName, setFullName] = useState('');
	const [error, setError] = useState<string | null>(null);

	useEffect(() => {
		let cancelled = false;
		getContractOffer()
			.then((result) => {
				if (cancelled) return;
				setOffer(result);
				setLoading(false);
			})
			.catch(() => {
				if (cancelled) return;
				setError('Không tải được Thoả thuận hợp tác.');
				setLoading(false);
			});
		return () => {
			cancelled = true;
		};
	}, []);

	const normalize = (value: string) => value.trim().replace(/\s+/g, ' ').toLowerCase();
	const nameMatches = offer
		? offer.expectedFullName
			? normalize(fullName) === normalize(offer.expectedFullName)
			: fullName.trim().length > 0
		: false;
	const canSubmit = !saving && scrolledToEnd && agreed && nameMatches;

	const onScroll = (event: React.UIEvent<HTMLDivElement>) => {
		if (scrolledToEnd) return;
		const el = event.currentTarget;
		if (el.scrollHeight - el.scrollTop - el.clientHeight < 24) setScrolledToEnd(true);
	};

	const submit = async () => {
		if (!offer || !canSubmit) return;
		setSaving(true);
		setError(null);
		try {
			await acceptContract({
				termsVersion: offer.termsVersion,
				termsHash: offer.termsHash,
				acceptedFullName: fullName.trim(),
			});
			await onDone();
		} catch {
			setError(
				'Không ghi nhận được chấp nhận. Điều khoản có thể đã được cập nhật — vui lòng tải lại bản mới nhất.'
			);
			setSaving(false);
		}
	};

	if (loading) {
		return (
			<StepCard icon={<FileSignature />} title="Thoả thuận hợp tác" description="Đang tải nội dung…">
				<div className="h-40 animate-pulse rounded-2xl bg-surface-container-highest/60" />
			</StepCard>
		);
	}

	if (!offer) {
		return (
			<StepCard icon={<FileSignature />} title="Thoả thuận hợp tác" description="Không tải được nội dung.">
				<p className="text-sm text-rose-600">{error}</p>
			</StepCard>
		);
	}

	return (
		<StepCard
			icon={<FileSignature />}
			title="Điều khoản hợp tác chuyên gia"
			description={`Số ${offer.contractNumber} · Phiên bản ${offer.termsVersion} · Thời hạn ${offer.termMonths} tháng · Cam kết ${offer.minSlotsPerWeek} ca/tuần`}
		>
			<div
				onScroll={onScroll}
				className="max-h-96 overflow-y-auto rounded-2xl border border-outline-variant/50 bg-surface p-5 text-[13px] leading-6 whitespace-pre-wrap text-on-surface-variant"
			>
				{offer.content}
			</div>

			{!scrolledToEnd && (
				<p className="mt-3 text-xs font-medium text-outline">
					Vui lòng đọc hết Thoả thuận để tiếp tục.
				</p>
			)}

			{scrolledToEnd && (
				<div className="mt-5 space-y-4">
					<label className="flex items-start gap-3 text-sm leading-6 text-on-surface">
						<input
							type="checkbox"
							checked={agreed}
							disabled={saving}
							onChange={(event) => setAgreed(event.target.checked)}
							className="mt-1 h-4 w-4 accent-primary"
						/>
						<span>
							Tôi đã đọc, hiểu và đồng ý với toàn bộ nội dung Thoả thuận hợp tác chuyên gia nêu trên.
						</span>
					</label>

					<div>
						<label className="mb-1.5 block text-xs font-semibold uppercase tracking-wider text-outline">
							Gõ lại họ và tên để xác nhận
						</label>
						<input
							value={fullName}
							disabled={saving}
							onChange={(event) => setFullName(event.target.value)}
							placeholder={offer.expectedFullName}
							className={`w-full rounded-xl border px-4 py-2.5 text-sm outline-none ${
								fullName.length === 0 || nameMatches ? 'border-outline-variant/60' : 'border-rose-400'
							}`}
						/>
						{offer.expectedFullName && (
							<p className="mt-1.5 text-xs text-outline">
								Đúng như hồ sơ đã duyệt: {offer.expectedFullName}
							</p>
						)}
					</div>
				</div>
			)}

			{error && <p className="mt-4 text-sm font-medium text-rose-600">{error}</p>}

			<button
				type="button"
				onClick={submit}
				disabled={!canSubmit}
				className="mt-6 inline-flex items-center gap-2 rounded-full bg-primary px-6 py-2.5 font-semibold text-on-primary disabled:opacity-40"
			>
				{saving ? 'Đang ghi nhận…' : 'Xác nhận chấp nhận'}
			</button>
		</StepCard>
	);
}
