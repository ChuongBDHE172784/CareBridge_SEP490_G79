import { useState, useEffect } from 'react';
import { getMyProfile, updateMyProfile } from '../services/expertApi';
import { updateUserProfile } from '../../auth/services/authApi';

export default function ExpertProfilePage() {
	const [profile, setProfile] = useState<any>(null);
	const [loading, setLoading] = useState(true);
	const [saving, setSaving] = useState(false);
	const [error, setError] = useState<string | null>(null);
	const [success, setSuccess] = useState(false);

	const [form, setForm] = useState({
		specialty: '',
		professionalTitle: '',
		experienceYears: '',
		workplace: '',
		consultationScope: '',
	});

	const [avatarUrl, setAvatarUrl] = useState('');
	const [avatarSaving, setAvatarSaving] = useState(false);
	const [avatarSuccess, setAvatarSuccess] = useState(false);

	const load = async () => {
		try {
			setError(null);
			const data = await getMyProfile();
			setProfile(data);
			setForm({
				specialty: data.specialty ?? '',
				professionalTitle: data.professionalTitle ?? '',
				experienceYears: data.experienceYears?.toString() ?? '',
				workplace: data.workplace ?? '',
				consultationScope: data.consultationScope ?? '',
			});
			setAvatarUrl(data.avatarUrl ?? '');
		} catch (e: any) {
			setError(e.response?.data?.message ?? 'Không thể tải hồ sơ');
		} finally {
			setLoading(false);
		}
	};

	useEffect(() => { load(); }, []);

	const onSubmit = async (e: React.FormEvent) => {
		e.preventDefault();
		setSaving(true);
		setSuccess(false);
		try {
			const body: any = {
				specialty: form.specialty,
				professionalTitle: form.professionalTitle,
				workplace: form.workplace,
				consultationScope: form.consultationScope,
			};
			if (form.experienceYears) body.experienceYears = parseInt(form.experienceYears);
			const updated = await updateMyProfile(body);
			setProfile(updated);
			setSuccess(true);
			setTimeout(() => setSuccess(false), 3000);
		} catch (e: any) {
			alert(e.response?.data?.message ?? 'Lưu thất bại');
		} finally {
			setSaving(false);
		}
	};

	const onSaveAvatar = async () => {
		setAvatarSaving(true);
		setAvatarSuccess(false);
		try {
			const updated = await updateUserProfile({ avatarUrl: avatarUrl || null });
			setProfile((prev: any) => ({ ...prev, avatarUrl: updated.avatarUrl }));
			setAvatarSuccess(true);
			setTimeout(() => setAvatarSuccess(false), 3000);
		} catch (e: any) {
			alert(e.response?.data?.message ?? 'Lưu ảnh thất bại');
		} finally {
			setAvatarSaving(false);
		}
	};

	if (loading) {
		return (
			<div className="flex items-center justify-center min-h-[400px]">
				<div className="animate-spin rounded-full h-8 w-8 border-2 border-primary border-t-transparent" />
			</div>
		);
	}

	const statusColor: Record<string, string> = {
		PENDING: 'bg-yellow-100 text-yellow-800',
		UNDER_REVIEW: 'bg-blue-100 text-blue-800',
		APPROVED: 'bg-green-100 text-green-800',
		VERIFIED: 'bg-green-100 text-green-800',
		REJECTED: 'bg-red-100 text-red-800',
		SUSPENDED: 'bg-gray-100 text-gray-800',
		EXPIRED: 'bg-gray-100 text-gray-800',
	};

	return (
		<div className="max-w-2xl mx-auto p-6">
			<div className="flex items-center justify-between mb-6">
				<h1 className="text-2xl font-bold text-on-surface">Hồ sơ chuyên môn</h1>
				<span className={`px-3 py-1 rounded-full text-sm font-medium ${statusColor[profile?.verificationStatus] || 'bg-gray-100'}`}>
					{profile?.verificationStatus ?? 'N/A'}
				</span>
			</div>

			{error && (
				<div className="mb-4 p-3 rounded bg-red-50 border border-red-200 text-red-700 text-sm">{error}</div>
			)}
			{success && (
				<div className="mb-4 p-3 rounded bg-green-50 border border-green-200 text-green-700 text-sm">Đã lưu thành công!</div>
			)}
			{avatarSuccess && (
				<div className="mb-4 p-3 rounded bg-green-50 border border-green-200 text-green-700 text-sm">Đã lưu ảnh thành công!</div>
			)}

			<div className="bg-white rounded-lg border border-gray-200 shadow-sm p-6 mb-4">
				<h3 className="text-sm font-bold text-gray-700 mb-3">Ảnh hồ sơ</h3>
				{avatarUrl && (
					<img
						src={avatarUrl}
						alt="Avatar preview"
						className="w-20 h-20 rounded-full object-cover border-2 border-gray-200 mb-3"
						onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }}
					/>
				)}
				<div className="flex gap-2">
					<input
						type="url"
						className="flex-1 rounded border border-gray-300 px-3 py-2 text-sm focus:border-primary focus:ring-1 focus:ring-primary"
						value={avatarUrl}
						onChange={(e) => setAvatarUrl(e.target.value)}
						placeholder="https://res.cloudinary.com/..."
					/>
					<button
						type="button"
						onClick={onSaveAvatar}
						disabled={avatarSaving}
						className="px-4 py-2 rounded bg-primary text-white text-sm font-medium hover:bg-primary/90 disabled:opacity-50 whitespace-nowrap"
					>
						{avatarSaving ? 'Đang lưu...' : 'Lưu ảnh'}
					</button>
				</div>
			</div>

			<div className="bg-white rounded-lg border border-gray-200 shadow-sm divide-y divide-gray-100">
				<form onSubmit={onSubmit} className="p-6 space-y-5">
					<Field label="Chuyên khoa" required>
						<input
							className="mt-1 block w-full rounded border border-gray-300 px-3 py-2 focus:border-primary focus:ring-1 focus:ring-primary"
							value={form.specialty}
							onChange={(e) => setForm({ ...form, specialty: e.target.value })}
							placeholder="VD: Sản khoa, Nhi khoa..."
						/>
					</Field>

					<Field label="Chức danh chuyên môn">
						<input
							className="mt-1 block w-full rounded border border-gray-300 px-3 py-2 focus:border-primary focus:ring-1 focus:ring-primary"
							value={form.professionalTitle}
							onChange={(e) => setForm({ ...form, professionalTitle: e.target.value })}
							placeholder="VD: Bác sĩ, Nha sĩ..."
						/>
					</Field>

					<Field label="Số năm kinh nghiệm">
						<input
							type="number"
							min="0"
							className="mt-1 block w-32 rounded border border-gray-300 px-3 py-2 focus:border-primary focus:ring-1 focus:ring-primary"
							value={form.experienceYears}
							onChange={(e) => setForm({ ...form, experienceYears: e.target.value })}
						/>
					</Field>

					<Field label="Nơi công tác">
						<input
							className="mt-1 block w-full rounded border border-gray-300 px-3 py-2 focus:border-primary focus:ring-1 focus:ring-primary"
							value={form.workplace}
							onChange={(e) => setForm({ ...form, workplace: e.target.value })}
							placeholder="VD: BV Phụ sản Hà Nội"
						/>
					</Field>

					<Field label="Lĩnh vực tư vấn">
						<textarea
							rows={4}
							className="mt-1 block w-full rounded border border-gray-300 px-3 py-2 focus:border-primary focus:ring-1 focus:ring-primary"
							value={form.consultationScope}
							onChange={(e) => setForm({ ...form, consultationScope: e.target.value })}
							placeholder="Mô tả lĩnh vực bạn có thể tư vấn..."
						/>
					</Field>

					{profile?.verifiedAt && (
						<p className="text-sm text-gray-500">
							Đã xác minh lúc: {new Date(profile.verifiedAt).toLocaleString('vi-VN')}
						</p>
					)}

					<div className="pt-2">
						<button
							type="submit"
							disabled={saving}
							className="px-5 py-2.5 rounded bg-primary text-white font-medium hover:bg-primary/90 disabled:opacity-50"
						>
							{saving ? 'Đang lưu...' : 'Lưu hồ sơ'}
						</button>
					</div>
				</form>
			</div>
		</div>
	);
}

function Field({ label, children, required }: { label: string; children: React.ReactNode; required?: boolean }) {
	return (
		<div>
			<label className="block text-sm font-medium text-gray-700">
				{label} {required && <span className="text-red-500">*</span>}
			</label>
			{children}
		</div>
	);
}
