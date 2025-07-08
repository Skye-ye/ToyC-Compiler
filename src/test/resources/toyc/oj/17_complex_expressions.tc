int factorial(int n)
{
    if (n <= 1)
    {
        return 1;
    }
    return n * factorial(n - 1);
}

int fibonacci(int n)
{
    if (n <= 0)
    {
        return 0;
    }
    if (n == 1)
    {
        return 1;
    }
    return fibonacci(n - 1) + fibonacci(n - 2);
}

int gcd(int a, int b)
{
    if (b == 0)
    {
        return a;
    }
    return gcd(b, a % b);
}

int is_prime(int n)
{
    if (n <= 1)
    {
        return 0;
    }
    if (n <= 3)
    {
        return 1;
    }
    if (n % 2 == 0 || n % 3 == 0)
    {
        return 0;
    }

    int i = 5;
    while (i * i <= n)
    {
        if (n % i == 0 || n % (i + 2) == 0)
        {
            return 0;
        }
        i = i + 6;
    }
    return 1;
}

int main()
{
    int a = 1;
    int b = 2;
    int c = 3;
    int d = 4;

    int expr1 = ((a + b) * c + (d * a)) / ((b + c) % (a + -d + 2048) + 1) + (a * b * c * (d - 2 - c));

    int x = 0;
    int y = 1;
    int z = 2;

    int expr2 = 0;
    if ((x > y) && ((z + 1) == 1))
    {
        expr2 = 1;
    }

    int expr3 = 0;
    if ((x < y) || ((z + 2) == 2))
    {
        expr3 = 1;
    }

    int expr4 = 0;
    if (!((x > 0 && y < 0) || (z > 0 && x < 0)) && (y > 0 || x < 0))
    {
        expr4 = 1;
    }

    int n1 = 42;
    int n2 = 56;
    int n3 = 87;

    int expr5 = factorial(gcd(n2, n3)) + fibonacci(n1 / 5);

    int max_val = 0;
    if (n1 > n2 && n1 > n3)
    {
        max_val = n1;
    }
    else if (n2 > n1 && n2 > n3)
    {
        max_val = n2;
    }
    else
    {
        max_val = n3;
    }

    int sum = 0;
    int i = 1;
    while (i <= 10)
    {
        if (i % 2 == 0)
        {
            sum = sum + i * i;
        }
        else if (i % 3 == 0)
        {
            sum = sum + i * i * i;
        }
        else
        {
            sum = sum + i;
        }
        i = i + 1;
    }

    int expr6 = 0;
    i = 1;
    while (i <= 5)
    {
        int j = 1;
        int term = 1;
        while (j <= i)
        {
            term = term * j;
            j = j + 1;
        }
        expr6 = expr6 + term;
        i = i + 1;
    }

    int expr7 = 0;
    if (is_prime(n1))
    {
        if (is_prime(n2))
        {
            expr7 = n1 * n2;
        }
        else if (is_prime(n3))
        {
            expr7 = n1 * n3;
        }
        else
        {
            expr7 = n1;
        }
    }
    else if (is_prime(n2))
    {
        if (is_prime(n3))
        {
            expr7 = n2 * n3;
        }
        else
        {
            expr7 = n2;
        }
    }
    else if (is_prime(n3))
    {
        expr7 = n3;
    }
    else
    {
        expr7 = n1 + n2 + n3;
    }

    int expr8 = 0;
    int num = 2345;
    int bit_count = 0;

    while (num > 0)
    {
        if (num % 2 == 1)
        {
            bit_count = bit_count + 1;
        }
        num = num / 2;
    }

    int final_result = expr1 + expr2 + expr3 + expr4 + expr5 + max_val + sum + expr6 + expr7 + bit_count;

    return final_result % 256;
}
