import LegalPageLayout, { Bullets, LawRef, P } from '../components/LegalPageLayout';
import type { LegalSection } from '../components/LegalPageLayout';

/**
 * Chính sách bảo mật dữ liệu cá nhân.
 *
 * Bố cục bám theo nghĩa vụ thông báo tại Điều 13 Nghị định 13/2023/NĐ-CP: phải
 * nêu rõ loại dữ liệu, mục đích, bên được xử lý, quyền và nghĩa vụ của chủ thể,
 * hậu quả rủi ro, và thời điểm bắt đầu/kết thúc xử lý. Mỗi mục dưới đây tương
 * ứng một yêu cầu đó, nên khi luật đổi thì sửa đúng mục, không phải viết lại cả
 * trang.
 *
 * Dữ liệu sức khỏe thuộc nhóm NHẠY CẢM, nên toàn bộ chính sách áp mức bảo vệ
 * cao hơn dữ liệu cơ bản và yêu cầu đồng ý tách bạch, có thông báo rõ.
 */

const CONTROLLER_NAME = 'Nhóm phát triển CareBridge — Đại học FPT (dự án SEP490_G79)';
const CONTACT_EMAIL = 'privacy@carebridgevn.site';

const sections: LegalSection[] = [
  {
    id: 'pham-vi',
    title: 'Phạm vi áp dụng',
    body: (
      <>
        <P>
          Chính sách này mô tả cách CareBridge thu thập, sử dụng, lưu trữ và bảo vệ dữ liệu cá nhân
          của người dùng trên nền tảng web và ứng dụng di động CareBridge, bao gồm chuyên gia y tế,
          người mẹ, thành viên gia đình và quản trị viên.
        </P>
        <P>
          Chính sách được xây dựng theo{' '}
          <LawRef>Luật Bảo vệ dữ liệu cá nhân số 91/2025/QH15</LawRef> (có hiệu lực từ 01/01/2026) và{' '}
          <LawRef>Nghị định số 13/2023/NĐ-CP</LawRef> về bảo vệ dữ liệu cá nhân.
        </P>
      </>
    ),
  },
  {
    id: 'ben-kiem-soat',
    title: 'Bên Kiểm soát dữ liệu cá nhân',
    body: (
      <>
        <P>
          Bên Kiểm soát dữ liệu cá nhân theo khoản 9 Điều 2 Nghị định 13/2023/NĐ-CP là{' '}
          <LawRef>{CONTROLLER_NAME}</LawRef>.
        </P>
        <P>
          Mọi yêu cầu liên quan đến dữ liệu cá nhân xin gửi về: <LawRef>{CONTACT_EMAIL}</LawRef>
        </P>
      </>
    ),
  },
  {
    id: 'du-lieu-thu-thap',
    title: 'Dữ liệu cá nhân được xử lý',
    body: (
      <>
        <P>
          <LawRef>Dữ liệu cá nhân cơ bản:</LawRef> họ tên, ngày sinh, giới tính, số điện thoại, địa
          chỉ email, ảnh đại diện, dữ liệu về hoạt động sử dụng dịch vụ và thiết bị truy cập.
        </P>
        <P>
          <LawRef>Dữ liệu cá nhân nhạy cảm:</LawRef> tình trạng sức khỏe, dữ liệu thai kỳ, dữ liệu
          theo dõi sức khỏe trẻ em, nội dung tư vấn y tế, dữ liệu vị trí trong tình huống khẩn cấp và
          dữ liệu sinh trắc học khuôn mặt phục vụ xác minh danh tính.
        </P>
        <P>
          Theo khoản 4 Điều 2 Nghị định 13/2023/NĐ-CP, thông tin về tình trạng sức khỏe và đời tư
          được ghi trong hồ sơ bệnh án thuộc nhóm dữ liệu cá nhân nhạy cảm. Vì vậy CareBridge{' '}
          <LawRef>thông báo rõ ràng</LawRef> cho bạn trước khi xử lý và chỉ xử lý khi có sự đồng ý
          riêng cho nhóm dữ liệu này.
        </P>
        <P>
          Riêng với chuyên gia y tế, CareBridge còn xử lý: số giấy phép hành nghề, phạm vi hoạt động
          chuyên môn, cơ sở công tác và các tài liệu chứng minh trình độ, nhằm đối soát điều kiện
          hành nghề theo <LawRef>Luật Khám bệnh, chữa bệnh số 15/2023/QH15</LawRef>.
        </P>
      </>
    ),
  },
  {
    id: 'muc-dich',
    title: 'Mục đích xử lý dữ liệu',
    body: (
      <Bullets
        items={[
          'Tạo và quản lý tài khoản, xác thực đăng nhập.',
          'Xác minh điều kiện hành nghề của chuyên gia y tế trước khi cho phép tư vấn.',
          'Cung cấp dịch vụ theo dõi thai kỳ, chăm sóc trẻ nhỏ và tư vấn sức khỏe.',
          'Kết nối người dùng với chuyên gia qua tin nhắn, cuộc gọi thoại và video.',
          'Hỗ trợ tình huống khẩn cấp, bao gồm chia sẻ vị trí tới người thân đã được chỉ định.',
          'Kiểm duyệt nội dung cộng đồng nhằm ngăn thông tin y tế sai lệch gây hại.',
          'Bảo đảm an toàn hệ thống, phát hiện gian lận và xử lý sự cố kỹ thuật.',
          'Thực hiện nghĩa vụ pháp lý khi cơ quan nhà nước có thẩm quyền yêu cầu.',
        ]}
      />
    ),
  },
  {
    id: 'can-cu-phap-ly',
    title: 'Căn cứ pháp lý của việc xử lý',
    body: (
      <>
        <P>CareBridge xử lý dữ liệu cá nhân dựa trên các căn cứ sau:</P>
        <Bullets
          items={[
            <>
              <LawRef>Sự đồng ý của chủ thể dữ liệu</LawRef> theo Điều 11 Nghị định 13/2023/NĐ-CP.
              Sự im lặng hoặc không phản hồi không được coi là đồng ý.
            </>,
            <>
              <LawRef>Thực hiện hợp đồng</LawRef> cung cấp dịch vụ giữa bạn và CareBridge.
            </>,
            <>
              <LawRef>Trường hợp không cần sự đồng ý</LawRef> theo Điều 17 Nghị định 13/2023/NĐ-CP,
              gồm: tình huống khẩn cấp nhằm bảo vệ tính mạng, sức khỏe của bạn hoặc người khác; thực
              hiện nghĩa vụ theo quy định của pháp luật; phục vụ hoạt động của cơ quan nhà nước có
              thẩm quyền.
            </>,
          ]}
        />
      </>
    ),
  },
  {
    id: 'thoi-gian-luu-tru',
    title: 'Thời gian xử lý và lưu trữ',
    body: (
      <>
        <P>
          Dữ liệu được xử lý kể từ thời điểm bạn đồng ý và kết thúc khi xảy ra một trong các trường
          hợp: bạn rút lại sự đồng ý, bạn yêu cầu xóa dữ liệu, tài khoản bị chấm dứt, hoặc mục đích
          xử lý đã hoàn thành.
        </P>
        <P>
          Sau thời điểm kết thúc, dữ liệu được xóa hoặc hủy theo Điều 16 Nghị định 13/2023/NĐ-CP, trừ
          phần dữ liệu phải lưu giữ lâu hơn theo yêu cầu của pháp luật chuyên ngành — ví dụ hồ sơ
          liên quan đến hoạt động khám bệnh, chữa bệnh phải lưu theo{' '}
          <LawRef>Điều 69 Luật Khám bệnh, chữa bệnh số 15/2023/QH15</LawRef>.
        </P>
      </>
    ),
  },
  {
    id: 'ben-thu-ba',
    title: 'Bên thứ ba tiếp nhận dữ liệu',
    body: (
      <>
        <P>
          CareBridge <LawRef>không mua bán dữ liệu cá nhân</LawRef>. Dữ liệu chỉ được chia sẻ với các
          bên xử lý phục vụ vận hành hệ thống, trong phạm vi tối thiểu cần thiết:
        </P>
        <Bullets
          items={[
            'Nhà cung cấp hạ tầng máy chủ và cơ sở dữ liệu.',
            'Dịch vụ lưu trữ tệp và hình ảnh y tế.',
            'Dịch vụ gọi thoại, gọi video phục vụ phiên tư vấn.',
            'Dịch vụ gửi thông báo đẩy và thư điện tử.',
            'Dịch vụ bản đồ phục vụ tính năng khẩn cấp và tìm cơ sở y tế.',
            'Dịch vụ trí tuệ nhân tạo phục vụ sàng lọc nội dung có nguy cơ gây hại.',
          ]}
        />
        <P>
          Chuyên gia y tế chỉ tiếp cận dữ liệu sức khỏe của người dùng đã chủ động gửi yêu cầu tư
          vấn, và có nghĩa vụ giữ bí mật theo{' '}
          <LawRef>Điều 45 Luật Khám bệnh, chữa bệnh số 15/2023/QH15</LawRef>.
        </P>
      </>
    ),
  },
  {
    id: 'quyen-chu-the',
    title: 'Quyền của bạn đối với dữ liệu cá nhân',
    body: (
      <>
        <P>Theo Điều 9 Nghị định 13/2023/NĐ-CP, bạn có các quyền sau:</P>
        <Bullets
          items={[
            'Quyền được biết về hoạt động xử lý dữ liệu cá nhân của mình.',
            'Quyền đồng ý hoặc không đồng ý cho phép xử lý dữ liệu.',
            'Quyền truy cập để xem, chỉnh sửa dữ liệu cá nhân của mình.',
            'Quyền rút lại sự đồng ý.',
            'Quyền xóa dữ liệu cá nhân.',
            'Quyền hạn chế xử lý dữ liệu.',
            'Quyền yêu cầu cung cấp dữ liệu cá nhân của mình.',
            'Quyền phản đối hoạt động xử lý dữ liệu.',
            'Quyền khiếu nại, tố cáo, khởi kiện theo quy định của pháp luật.',
            'Quyền yêu cầu bồi thường thiệt hại khi có vi phạm.',
            'Quyền tự bảo vệ theo quy định của Bộ luật Dân sự.',
          ]}
        />
        <P>
          Yêu cầu thực hiện quyền xin gửi tới <LawRef>{CONTACT_EMAIL}</LawRef>. CareBridge phản hồi
          trong thời hạn <LawRef>72 giờ</LawRef> theo quy định tại Điều 14 và Điều 15 Nghị định
          13/2023/NĐ-CP.
        </P>
      </>
    ),
  },
  {
    id: 'rut-dong-y',
    title: 'Rút lại sự đồng ý',
    body: (
      <>
        <P>
          Theo Điều 12 Nghị định 13/2023/NĐ-CP, bạn có thể rút lại sự đồng ý bất cứ lúc nào mà không
          ảnh hưởng đến tính hợp pháp của việc xử lý đã thực hiện trước đó.
        </P>
        <P>
          Cần lưu ý: nhiều chức năng cốt lõi của CareBridge phụ thuộc vào dữ liệu sức khỏe. Khi bạn
          rút lại sự đồng ý đối với nhóm dữ liệu nhạy cảm, các chức năng tư vấn y tế, theo dõi thai
          kỳ và hỗ trợ khẩn cấp sẽ ngừng hoạt động.
        </P>
      </>
    ),
  },
  {
    id: 'nghia-vu-chu-the',
    title: 'Nghĩa vụ của bạn',
    body: (
      <>
        <P>Theo Điều 10 Nghị định 13/2023/NĐ-CP, bạn có nghĩa vụ:</P>
        <Bullets
          items={[
            'Tự bảo vệ dữ liệu cá nhân của mình, giữ bí mật thông tin đăng nhập.',
            'Tôn trọng và bảo vệ dữ liệu cá nhân của người khác.',
            'Cung cấp thông tin trung thực, chính xác khi đăng ký và sử dụng dịch vụ.',
            'Thông báo cho CareBridge khi phát hiện dữ liệu của mình bị xâm phạm.',
          ]}
        />
        <P>
          Với chuyên gia y tế, việc khai báo sai giấy phép hành nghề có thể bị xử lý theo pháp luật
          về khám bệnh, chữa bệnh, ngoài việc bị chấm dứt tài khoản trên CareBridge.
        </P>
      </>
    ),
  },
  {
    id: 'bao-mat',
    title: 'Biện pháp bảo vệ dữ liệu',
    body: (
      <Bullets
        items={[
          'Mã hóa đường truyền bằng TLS cho toàn bộ kết nối giữa thiết bị và máy chủ.',
          'Mật khẩu được băm một chiều, hệ thống không lưu mật khẩu dạng đọc được.',
          'Phân quyền theo vai trò: mỗi vai trò chỉ truy cập được phần dữ liệu thuộc phạm vi công việc.',
          'Ghi nhật ký truy cập dữ liệu nhạy cảm phục vụ kiểm toán.',
          'Xác thực hai bước bằng mã OTP khi đăng nhập.',
          'Rà soát lỗ hổng bảo mật và cập nhật thư viện định kỳ trong quy trình phát triển.',
        ]}
      />
    ),
  },
  {
    id: 'du-lieu-tre-em',
    title: 'Dữ liệu cá nhân của trẻ em',
    body: (
      <P>
        CareBridge có tính năng theo dõi sức khỏe trẻ nhỏ. Dữ liệu của trẻ em dưới 16 tuổi chỉ được
        xử lý khi có sự đồng ý của cha, mẹ hoặc người giám hộ, theo Điều 20 Nghị định 13/2023/NĐ-CP.
        Người tạo hồ sơ trẻ em phải bảo đảm mình có quyền giám hộ hợp pháp đối với trẻ.
      </P>
    ),
  },
  {
    id: 'su-co',
    title: 'Xử lý khi xảy ra sự cố lộ dữ liệu',
    body: (
      <P>
        Khi phát hiện dữ liệu cá nhân bị xâm phạm, CareBridge thông báo cho cơ quan chuyên trách bảo
        vệ dữ liệu cá nhân (Bộ Công an) chậm nhất <LawRef>72 giờ</LawRef> kể từ khi phát hiện, theo
        Điều 23 Nghị định 13/2023/NĐ-CP, đồng thời thông báo cho người dùng bị ảnh hưởng và triển
        khai biện pháp khắc phục.
      </P>
    ),
  },
  {
    id: 'thay-doi',
    title: 'Thay đổi chính sách',
    body: (
      <P>
        CareBridge có thể cập nhật chính sách này khi quy định pháp luật hoặc tính năng hệ thống thay
        đổi. Bản cập nhật được đăng tại trang này kèm ngày hiệu lực mới. Với thay đổi ảnh hưởng đáng
        kể tới quyền của bạn, CareBridge sẽ thông báo trong ứng dụng và xin lại sự đồng ý nếu pháp
        luật yêu cầu.
      </P>
    ),
  },
  {
    id: 'can-cu-van-ban',
    title: 'Danh mục văn bản pháp luật áp dụng',
    body: (
      <>
        <Bullets
          items={[
            <>
              <LawRef>Luật Bảo vệ dữ liệu cá nhân số 91/2025/QH15</LawRef> — hiệu lực 01/01/2026.
            </>,
            <>
              <LawRef>Nghị định số 13/2023/NĐ-CP</LawRef> ngày 17/4/2023 về bảo vệ dữ liệu cá nhân —
              Điều 2, 9, 10, 11, 12, 13, 16, 17, 20, 23.
            </>,
            <>
              <LawRef>Luật Khám bệnh, chữa bệnh số 15/2023/QH15</LawRef> — Điều 10 (quyền được tôn
              trọng bí mật riêng tư), Điều 45 (nghĩa vụ giữ bí mật của người hành nghề), Điều 69 (hồ
              sơ bệnh án).
            </>,
            <>
              <LawRef>Luật An toàn thông tin mạng số 86/2015/QH13</LawRef> — Điều 16 đến Điều 19 về
              bảo vệ thông tin cá nhân trên mạng.
            </>,
            <>
              <LawRef>Luật An ninh mạng số 24/2018/QH14</LawRef> — Điều 26 về bảo đảm an ninh thông
              tin trên không gian mạng.
            </>,
            <>
              <LawRef>Luật Giao dịch điện tử số 20/2023/QH15</LawRef> — giá trị pháp lý của việc chấp
              thuận bằng phương tiện điện tử.
            </>,
            <>
              <LawRef>Luật Bảo vệ quyền lợi người tiêu dùng số 19/2023/QH15</LawRef> — nghĩa vụ bảo
              vệ thông tin của người tiêu dùng.
            </>,
            <>
              <LawRef>Bộ luật Dân sự số 91/2015/QH13</LawRef> — Điều 38 về quyền đối với đời sống
              riêng tư, bí mật cá nhân, bí mật gia đình.
            </>,
          ]}
        />
      </>
    ),
  },
];

export default function PrivacyPolicyPage() {
  return (
    <LegalPageLayout
      documentTitle="Chính sách bảo mật dữ liệu cá nhân"
      subtitle="CareBridge xử lý dữ liệu sức khỏe — thuộc nhóm dữ liệu cá nhân nhạy cảm theo pháp luật Việt Nam. Tài liệu này nêu rõ chúng tôi thu thập gì, dùng để làm gì, chia sẻ với ai, và bạn có những quyền nào."
      effectiveDate="14/08/2026"
      version="1.0"
      sections={sections}
    />
  );
}
