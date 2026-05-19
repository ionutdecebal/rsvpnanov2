#pragma once

#include <Arduino.h>

class BookWordSource {
 public:
  virtual ~BookWordSource() = default;

  virtual size_t wordCount() const = 0;
  virtual String wordAt(size_t index) const = 0;
  virtual void prefetchAround(size_t index) const { (void)index; }
};
