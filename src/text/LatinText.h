#pragma once

#include <Arduino.h>
#include <stdint.h>

namespace LatinText {

inline uint8_t byteValue(char c) { return static_cast<uint8_t>(c); }

inline bool isRepurposedLatin1Byte(uint8_t value) {
  switch (value) {
    case 0xA1:
    case 0xA2:
    case 0xA3:
    case 0xA4:
    case 0xA5:
    case 0xA6:
    case 0xA7:
    case 0xA8:
    case 0xA9:
    case 0xAA:
    case 0xAB:
    case 0xAC:
    case 0xAE:
    case 0xAF:
    case 0xB0:
    case 0xB1:
    case 0xB2:
    case 0xB3:
    case 0xB4:
    case 0xB5:
    case 0xB6:
    case 0xB7:
    case 0xB8:
    case 0xB9:
    case 0xBA:
    case 0xBB:
    case 0xBC:
    case 0xBD:
    case 0xBE:
    case 0xBF:
    case 0xD7:
    case 0xF7:
      return true;
    default:
      return false;
  }
}

inline bool customSlotForCodepoint(uint32_t codepoint, uint8_t &slot) {
  switch (codepoint) {
    case 0x0152:
      slot = 0x80;
      return true;
    case 0x0153:
      slot = 0x81;
      return true;
    case 0x0141:
      slot = 0x82;
      return true;
    case 0x0142:
      slot = 0x83;
      return true;
    case 0x010C:
      slot = 0x84;
      return true;
    case 0x010D:
      slot = 0x85;
      return true;
    case 0x0160:
      slot = 0x86;
      return true;
    case 0x0161:
      slot = 0x87;
      return true;
    case 0x017D:
      slot = 0x88;
      return true;
    case 0x017E:
      slot = 0x89;
      return true;
    case 0x0102:
      slot = 0x8A;
      return true;
    case 0x0103:
      slot = 0x8B;
      return true;
    case 0x0218:
      slot = 0x8C;
      return true;
    case 0x0219:
      slot = 0x8D;
      return true;
    case 0x021A:
      slot = 0x8E;
      return true;
    case 0x021B:
      slot = 0x8F;
      return true;
    case 0x011E:
      slot = 0x90;
      return true;
    case 0x011F:
      slot = 0x91;
      return true;
    case 0x015E:
      slot = 0x92;
      return true;
    case 0x015F:
      slot = 0x93;
      return true;
    case 0x0130:
      slot = 0x94;
      return true;
    case 0x0131:
      slot = 0x95;
      return true;
    case 0x0104:
      slot = 0x96;
      return true;
    case 0x0105:
      slot = 0x97;
      return true;
    case 0x0118:
      slot = 0x98;
      return true;
    case 0x0119:
      slot = 0x99;
      return true;
    case 0x0106:
      slot = 0x9A;
      return true;
    case 0x0107:
      slot = 0x9B;
      return true;
    case 0x0143:
      slot = 0x9C;
      return true;
    case 0x0144:
      slot = 0x9D;
      return true;
    case 0x015A:
      slot = 0x9E;
      return true;
    case 0x015B:
      slot = 0x9F;
      return true;
    case 0x0179:
      slot = 0xB2;
      return true;
    case 0x017A:
      slot = 0xB3;
      return true;
    case 0x017B:
      slot = 0xB4;
      return true;
    case 0x017C:
      slot = 0xB5;
      return true;
    case 0x0100:
      slot = 0xA1;
      return true;
    case 0x0101:
      slot = 0xA2;
      return true;
    case 0x0112:
      slot = 0xA3;
      return true;
    case 0x0113:
      slot = 0xA4;
      return true;
    case 0x0122:
      slot = 0xA5;
      return true;
    case 0x0123:
      slot = 0xA6;
      return true;
    case 0x012A:
      slot = 0xA7;
      return true;
    case 0x012B:
      slot = 0xA8;
      return true;
    case 0x0136:
      slot = 0xA9;
      return true;
    case 0x0137:
      slot = 0xAA;
      return true;
    case 0x013B:
      slot = 0xAB;
      return true;
    case 0x013C:
      slot = 0xAC;
      return true;
    case 0x0145:
      slot = 0xAE;
      return true;
    case 0x0146:
      slot = 0xAF;
      return true;
    case 0x0116:
      slot = 0xB0;
      return true;
    case 0x0117:
      slot = 0xB1;
      return true;
    case 0x012E:
      slot = 0xB6;
      return true;
    case 0x012F:
      slot = 0xB7;
      return true;
    case 0x0172:
      slot = 0xB8;
      return true;
    case 0x0173:
      slot = 0xB9;
      return true;
    case 0x016A:
      slot = 0xBA;
      return true;
    case 0x016B:
      slot = 0xBB;
      return true;
    case 0x0110:
      slot = 0xBC;
      return true;
    case 0x0111:
      slot = 0xBD;
      return true;
    case 0x014A:
      slot = 0xBE;
      return true;
    case 0x014B:
      slot = 0xBF;
      return true;
    case 0x0166:
      slot = 0xD7;
      return true;
    case 0x0167:
      slot = 0xF7;
      return true;
    default:
      return false;
  }
}

inline bool directStorageByteForCodepoint(uint32_t codepoint, uint8_t &value) {
  if (codepoint < 0x00A1 || codepoint > 0x00FF) {
    return false;
  }

  const uint8_t byte = static_cast<uint8_t>(codepoint);
  if (isRepurposedLatin1Byte(byte)) {
    return false;
  }

  value = byte;
  return true;
}

inline bool storageByteForCodepoint(uint32_t codepoint, uint8_t &value) {
  if (codepoint >= 32 && codepoint <= 126) {
    value = static_cast<uint8_t>(codepoint);
    return true;
  }
  if (customSlotForCodepoint(codepoint, value)) {
    return true;
  }
  return directStorageByteForCodepoint(codepoint, value);
}

inline bool customLowercaseByte(uint8_t value, uint8_t &lowercase) {
  switch (value) {
    case 0x80:
      lowercase = 0x81;
      return true;
    case 0x82:
      lowercase = 0x83;
      return true;
    case 0x84:
      lowercase = 0x85;
      return true;
    case 0x86:
      lowercase = 0x87;
      return true;
    case 0x88:
      lowercase = 0x89;
      return true;
    case 0x8A:
      lowercase = 0x8B;
      return true;
    case 0x8C:
      lowercase = 0x8D;
      return true;
    case 0x8E:
      lowercase = 0x8F;
      return true;
    case 0x90:
      lowercase = 0x91;
      return true;
    case 0x92:
      lowercase = 0x93;
      return true;
    case 0x94:
      lowercase = 0x95;
      return true;
    case 0x96:
      lowercase = 0x97;
      return true;
    case 0x98:
      lowercase = 0x99;
      return true;
    case 0x9A:
      lowercase = 0x9B;
      return true;
    case 0x9C:
      lowercase = 0x9D;
      return true;
    case 0x9E:
      lowercase = 0x9F;
      return true;
    case 0xA1:
      lowercase = 0xA2;
      return true;
    case 0xA3:
      lowercase = 0xA4;
      return true;
    case 0xA5:
      lowercase = 0xA6;
      return true;
    case 0xA7:
      lowercase = 0xA8;
      return true;
    case 0xA9:
      lowercase = 0xAA;
      return true;
    case 0xAB:
      lowercase = 0xAC;
      return true;
    case 0xAE:
      lowercase = 0xAF;
      return true;
    case 0xB0:
      lowercase = 0xB1;
      return true;
    case 0xB2:
      lowercase = 0xB3;
      return true;
    case 0xB4:
      lowercase = 0xB5;
      return true;
    case 0xB6:
      lowercase = 0xB7;
      return true;
    case 0xB8:
      lowercase = 0xB9;
      return true;
    case 0xBA:
      lowercase = 0xBB;
      return true;
    case 0xBC:
      lowercase = 0xBD;
      return true;
    case 0xBE:
      lowercase = 0xBF;
      return true;
    case 0xD7:
      lowercase = 0xF7;
      return true;
    default:
      return false;
  }
}

inline bool isCustomUppercaseLetter(uint8_t value) {
  uint8_t lowered = 0;
  return customLowercaseByte(value, lowered);
}

inline bool isCustomLowercaseLetter(uint8_t value) {
  switch (value) {
    case 0x81:
    case 0x83:
    case 0x85:
    case 0x87:
    case 0x89:
    case 0x8B:
    case 0x8D:
    case 0x8F:
    case 0x91:
    case 0x93:
    case 0x95:
    case 0x97:
    case 0x99:
    case 0x9B:
    case 0x9D:
    case 0x9F:
    case 0xA2:
    case 0xA4:
    case 0xA6:
    case 0xA8:
    case 0xAA:
    case 0xAC:
    case 0xAF:
    case 0xB1:
    case 0xB3:
    case 0xB5:
    case 0xB7:
    case 0xB9:
    case 0xBB:
    case 0xBD:
    case 0xBF:
    case 0xF7:
      return true;
    default:
      return false;
  }
}

inline bool isDigit(uint8_t value) { return value >= '0' && value <= '9'; }

inline bool isUppercaseLetter(uint8_t value) {
  return (value >= 'A' && value <= 'Z') || (value >= 0xC0 && value <= 0xD6) ||
         (value >= 0xD8 && value <= 0xDE) || isCustomUppercaseLetter(value);
}

inline bool isLowercaseLetter(uint8_t value) {
  return (value >= 'a' && value <= 'z') || value == 0xDF ||
         (value >= 0xE0 && value <= 0xF6) || (value >= 0xF8 && value <= 0xFF) ||
         isCustomLowercaseLetter(value);
}

inline bool isLetter(uint8_t value) {
  return isUppercaseLetter(value) || isLowercaseLetter(value);
}

inline bool isWordCharacter(uint8_t value) { return isLetter(value) || isDigit(value); }

inline uint8_t toLowercaseByte(uint8_t value) {
  if (value >= 'A' && value <= 'Z') {
    return static_cast<uint8_t>(value + 32);
  }
  if ((value >= 0xC0 && value <= 0xD6) || (value >= 0xD8 && value <= 0xDE)) {
    return static_cast<uint8_t>(value + 32);
  }
  uint8_t lowercase = 0;
  if (customLowercaseByte(value, lowercase)) {
    return lowercase;
  }
  return value;
}

inline bool isVowel(uint8_t value) {
  switch (toLowercaseByte(value)) {
    case 'a':
    case 'e':
    case 'i':
    case 'o':
    case 'u':
    case 'y':
    case 0xE0:
    case 0xE1:
    case 0xE2:
    case 0xE3:
    case 0xE4:
    case 0xE5:
    case 0xE6:
    case 0xE8:
    case 0xE9:
    case 0xEA:
    case 0xEB:
    case 0xEC:
    case 0xED:
    case 0xEE:
    case 0xEF:
    case 0xF2:
    case 0xF3:
    case 0xF4:
    case 0xF5:
    case 0xF6:
    case 0xF8:
    case 0xF9:
    case 0xFA:
    case 0xFB:
    case 0xFC:
    case 0xFD:
    case 0xFF:
    case 0x81:
    case 0x8B:
    case 0x95:
    case 0x97:
    case 0x99:
    case 0xA2:
    case 0xA4:
    case 0xA8:
    case 0xB1:
    case 0xB7:
    case 0xB9:
    case 0xBB:
      return true;
    default:
      return false;
  }
}

inline bool hasExtendedBytes(const String &text) {
  for (size_t i = 0; i < text.length(); ++i) {
    if (byteValue(text[i]) > 126) {
      return true;
    }
  }
  return false;
}

}  // namespace LatinText
